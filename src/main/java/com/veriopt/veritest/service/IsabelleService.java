package com.veriopt.veritest.service;

import com.veriopt.veritest.isabelle.config.SessionConfig;
import com.veriopt.veritest.dto.IsabelleResult;
import com.veriopt.veritest.dto.Status;
import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.errors.IsabelleException;
import com.veriopt.veritest.isabelle.IsabelleClient;
import com.veriopt.veritest.isabelle.command.Command;
import com.veriopt.veritest.isabelle.command.TheoryFileTemplate;
import com.veriopt.veritest.isabelle.request.SessionStartRequest;
import com.veriopt.veritest.isabelle.request.SessionStopRequest;
import com.veriopt.veritest.isabelle.request.UseTheoryRequest;
import com.veriopt.veritest.isabelle.response.IsabelleGenericError;
import com.veriopt.veritest.isabelle.response.SessionStartResponse;
import com.veriopt.veritest.isabelle.response.TaskMessage;
import com.veriopt.veritest.isabelle.response.TheoryResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

@Log4j2
@Service
public class IsabelleService {
    private static final String QUICKCHECK_FOUND_STRING = "Quickcheck found a counterexample.";
    private static final String NITPICK_FOUND_STRING = "Nitpick found a counterexample:";
    private static final String SLEDGEHAMMER_FOUND_STRING = "Try this:";
    private static final String SLEDGEHAMMER_DONE = "No proof state";
    public static final String NO_SUBGOAL = "No subgoal!";

    private final @NonNull IsabelleClient client;
    private final @NonNull SessionConfig sessionConfig;
    private final @NonNull TheoryFileTemplate fileTemplate;

    @Autowired
    public IsabelleService(@NonNull IsabelleClient client,
                           @NonNull SessionConfig sessionConfig,
                           @NonNull TheoryFileTemplate fileTemplate) {
        this.client = client;
        this.sessionConfig = sessionConfig;
        this.fileTemplate = fileTemplate;
    }

    private CompletableFuture<Void> allOfReturnOnSuccess(List<CompletableFuture<IsabelleResult>> futures) {
        final CompletableFuture<Void> futureResult = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        futures.forEach(future -> future.thenAccept(result -> {
            if(Future.State.SUCCESS.equals(future.state())
                    && !Status.FAILED.equals(result.getStatus())) {
                futureResult.complete(null);
            }
        }));

        return futureResult;
    }

    public IsabelleResult validateTheory(@Valid @NotNull TheoryRequest request) {
        List<CompletableFuture<IsabelleResult>> futures = Arrays.asList(
                tryAutoProof(request),
                tryCounterexamples(request),
                trySledgehammer(request)
        );

        try {
            allOfReturnOnSuccess(futures).get();
        } catch (ExecutionException e) {
            switch (e.getCause()) {
                case IsabelleException error -> throw error;
                case InterruptedException error -> throw new IsabelleException("Process is interrupted", error);
                default -> throw new IsabelleException("Execution Error", e.getCause());
            }
        } catch (InterruptedException | CancellationException e) {
            log.error(e.getMessage(), e);

            throw new IsabelleException("Process is interrupted", e);
        }

        return futures.stream()
                .parallel()
                .filter(future -> Future.State.SUCCESS.equals(future.state()))
                .map(CompletableFuture::resultNow)
                .filter(result -> !Status.FAILED.equals(result.getStatus()))
                .findAny()
                .orElse(futures.getFirst().join());
    }

    private static File generateFile(String generatedTheory, Path path) throws IOException {
        File file = path.toFile();
        if (file.exists() || !file.createNewFile()) {
            throw new IOException("File Cannot be created");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file.getAbsoluteFile()))) {
            writer.print(generatedTheory);
        }

        return file;
    }

    private record TheorySubmission(File file, SessionStartResponse response) { }

    private CompletableFuture<TheoryResponse> submitTheory(String generatedTheory) {
        return this.client.startSession(new SessionStartRequest(this.sessionConfig))
                .thenApplyAsync(task -> switch (task) {
                    case IsabelleGenericError error -> throw new IsabelleException(error, "Session start error");
                    case SessionStartResponse startResponse -> startResponse;
                    default -> throw new IllegalStateException("Unexpected value: " + task);
                })
                .thenApplyAsync(response -> {
//                    Path tempDir = Path.of("\\\\wsl$\\Ubuntu"); // TODO: temp fix for windows env
//                    tempDir = tempDir.resolve(response.getTempDir());
                    Path tempDir = Path.of(response.getTempDir());

                    Path filePath = tempDir.resolve(TheoryFileTemplate.theoryFilename());
                    try {
                        File file = generateFile(generatedTheory, filePath);
                        log.debug("Theory: \n{}", generatedTheory);

                        return new TheorySubmission(file, response);
                    } catch (IOException e) {
                        throw new IsabelleException(e.getMessage(), e);
                    }
                })
                .thenComposeAsync(theorySubmission -> {
                    final String sessionId = theorySubmission.response().getSessionId();

                    return CompletableFuture.supplyAsync(
                                    () -> UseTheoryRequest.builder()
                                            .sessionID(sessionId)
                                            .theories(Collections.singletonList(TheoryFileTemplate.theoryName()))
                                            .build()
                            )
                            .thenComposeAsync(client::useTheory)
                            .thenApplyAsync(task -> switch (task) {
                                case TheoryResponse theoryResponse -> theoryResponse;
                                case IsabelleGenericError error ->
                                        throw new IsabelleException(error, "Isabelle submit theory error");
                                default -> throw new IllegalStateException("Unexpected value: " + task);
                            })
                            .whenCompleteAsync(cleanupTheory(theorySubmission, sessionId));
                });
    }

    private BiConsumer<TheoryResponse, Throwable> cleanupTheory(TheorySubmission theorySubmission, String sessionId) {
        return (theoryResponse, throwable) -> {
            boolean ignored = theorySubmission.file().delete();

            Thread stopThread = new Thread(() -> {
                SessionStopRequest request = SessionStopRequest.builder()
                        .sessionID(sessionId)
                        .build();

                try {
                    client.stopSession(request).join();
                } catch (Throwable e) {
                    log.error(String.format("Session %s failed to stop", sessionId), e);
                }
            }, String.format("Stop-Thread-%s", sessionId.substring(24)));

            stopThread.start();
        };
    }

    private CompletableFuture<IsabelleResult> tryAutoProof(@Valid @NotNull TheoryRequest request) {
        log.debug("Auto Proof for ID = {}", request.getRequestId());

        return CompletableFuture.supplyAsync(() ->
                        fileTemplate.generate(Command.AUTO, request))
                .thenComposeAsync(this::submitTheory)
                .whenCompleteAsync((theoryResponse, ignored) ->
                        log.debug("Get Response for Auto: {}", theoryResponse))
                .thenApplyAsync(theoryResponse -> buildAutoResult(request, theoryResponse));
    }

    private static IsabelleResult handleErrorResult(TheoryRequest request, TheoryResponse theoryResponse) {
        List<String> messages = theoryResponse.getErrors().stream()
                .parallel()
                .filter(taskMessage -> "error".equals(taskMessage.getKind()))
                .map(TaskMessage::getMessage)
                .toList();

        return IsabelleResult.builder()
                .requestID(request.getRequestId())
                .status(Status.FAILED)
                .isabelleMessages(messages)
                .build();
    }

    private static IsabelleResult buildAutoResult(TheoryRequest request, TheoryResponse theoryResponse) {
        if (!theoryResponse.getOk()) {
            return handleErrorResult(request, theoryResponse);
        }

        return IsabelleResult.builder()
                .requestID(request.getRequestId())
                .status(Status.FOUND_AUTO_PROOF)
                .build();
    }

    private CompletableFuture<IsabelleResult> tryCounterexamples(@Valid @NotNull TheoryRequest request) {
        return tryQuickcheck(request)
                .thenCombine(tryNitpick(request), (quickcheckResult, nitpickResult) -> {
                    // Prioritize Nitpick results
                    if (!Status.FAILED.equals(nitpickResult.getStatus())) {
                        return nitpickResult;
                    }

                    if (!Status.FAILED.equals(quickcheckResult.getStatus())) {
                        return quickcheckResult;
                    }

                    return IsabelleResult.builder()
                        .requestID(request.getRequestId())
                        .status(Status.FAILED)
                        .build();
                });
    }

    private CompletableFuture<IsabelleResult> tryNitpick(@Valid @NotNull TheoryRequest request) {
        log.debug("Nitpick for ID = {}", request.getRequestId());

        return CompletableFuture.supplyAsync(() -> fileTemplate.generate(Command.NITPICK, request))
                .thenComposeAsync(this::submitTheory)
                .whenCompleteAsync((theoryResponse, ignored) ->
                        log.debug("Get Response for Nitpick: {}", theoryResponse))
                .thenApplyAsync(theoryResponse ->
                        buildCounterexampleResult(request, theoryResponse, NITPICK_FOUND_STRING));
    }

    private CompletableFuture<IsabelleResult> tryQuickcheck(@Valid @NotNull TheoryRequest request) {
        log.debug("Quickcheck for ID = {}", request.getRequestId());

        return CompletableFuture.supplyAsync(() -> fileTemplate.generate(Command.QUICKCHECK, request))
                .thenComposeAsync(this::submitTheory)
                .whenCompleteAsync((theoryResponse, ignored) ->
                        log.debug("Get Response for Quickcheck: {}", theoryResponse))
                .thenApplyAsync(theoryResponse ->
                        buildCounterexampleResult(request, theoryResponse, QUICKCHECK_FOUND_STRING));
    }

    /* Task Processing */
    private static IsabelleResult buildCounterexampleResult(TheoryRequest request,
                                                            TheoryResponse theoryResponse,
                                                            String foundString) {
        if (!theoryResponse.getOk()) {
            return handleErrorResult(request, theoryResponse);
        }

        List<TaskMessage> messages = theoryResponse.getNodes().getFirst().getMessages();
        List<String> counterexamples = messages.stream()
                .parallel()
                .map(TaskMessage::getMessage)
                .filter(message -> message.contains(foundString))
                .toList();

        if (counterexamples.isEmpty()) {
            return IsabelleResult.builder()
                    .requestID(request.getRequestId())
                    .status(Status.FAILED)
                    .build();
        }

        return IsabelleResult.builder()
                .requestID(request.getRequestId())
                .status(Status.FOUND_COUNTEREXAMPLE)
                .counterexample(counterexamples.getFirst())
                .build();
    }

    /*
    * Done -> No proof state
    * In Progress -> ___: Try this: {proof}
    * No Proof -> No try this and not done
    * */
    private CompletableFuture<IsabelleResult> trySledgehammer(@Valid @NotNull TheoryRequest request) {
        log.debug("Sledgehammer for ID = {}", request.getRequestId());
        return CompletableFuture.supplyAsync(() -> fileTemplate.generate(Command.SLEDGEHAMMER, request))
                .thenComposeAsync(this::submitTheory)
                .whenCompleteAsync((theoryResponse, ignored) -> log.debug("Get Response for Sledgehammer: {}", theoryResponse))
                .thenApplyAsync(theoryResponse -> recursiveSledgehammer(request, theoryResponse));
    }

    private IsabelleResult recursiveSledgehammer(TheoryRequest request, TheoryResponse theoryResponse) {
        List<TaskMessage> messages = theoryResponse.getNodes().getFirst().getMessages();

        long done = messages.stream()
                .parallel()
                .map(TaskMessage::getMessage)
                .filter(message -> message.contains(SLEDGEHAMMER_DONE) || message.contains(NO_SUBGOAL))
                .count();

        /* Base Case: Found Proof */
        if (done > 0) {
            return IsabelleResult.builder()
                    .requestID(request.getRequestId())
                    .status(Status.FOUND_PROOF)
                    .proofs(request.getProofs())
                    .build();
        }

        List<String> proofs = messages.stream()
                .parallel()
                .map(TaskMessage::getMessage)
                .filter(message -> message.contains(SLEDGEHAMMER_FOUND_STRING))
                .map(message -> {
                    String s = message.substring(
                            message.indexOf(SLEDGEHAMMER_FOUND_STRING) + SLEDGEHAMMER_FOUND_STRING.length(),
                            message.lastIndexOf('(')
                    );

                    return s.trim();
                })
                .toList();

        /* Base Case: Proof not found */
        if (proofs.isEmpty()) {
            return IsabelleResult.builder()
                    .requestID(request.getRequestId())
                    .status(Status.FAILED)
                    .message("Proof not found")
                    .build();
        }

        /* Base Case: Error in processing */
        if (!theoryResponse.getOk()) {
            return handleErrorResult(request, theoryResponse);
        }

        /* Recursion: Try proofs */
        List<CompletableFuture<IsabelleResult>> futures = new ArrayList<>();
        for (String proof : proofs) {
            TheoryRequest childRequest = TheoryRequest.builder()
                    .requestId(request.getRequestId())
                    .theory(request.getTheory())
                    .build();

            if (request.getProofs() == null) {
                childRequest.setProofs(new ArrayList<>());
            } else {
                childRequest.setProofs(new ArrayList<>(request.getProofs()));
            }

            childRequest.getProofs().add(proof);

            futures.add(trySledgehammer(childRequest));
        }

        allOfReturnOnSuccess(futures).join();

        /* Recursion end: Propagate successful proofs */
        List<IsabelleResult> results = futures.stream()
                .parallel()
                .filter(future -> Future.State.SUCCESS.equals(future.state()))
                .map(CompletableFuture::resultNow)
                .filter(isabelleResult -> Status.FOUND_PROOF.equals(isabelleResult.getStatus()))
                .toList();

        if (results.isEmpty()) {
            return IsabelleResult.builder()
                    .requestID(request.getRequestId())
                    .status(Status.FAILED)
                    .message("Proof not found")
                    .build();
        }

        return results.getFirst();
    }
}
