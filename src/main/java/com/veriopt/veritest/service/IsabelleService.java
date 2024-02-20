package com.veriopt.veritest.service;

import com.veriopt.veritest.config.SessionConfig;
import com.veriopt.veritest.dto.IsabelleResult;
import com.veriopt.veritest.dto.Status;
import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.isabelle.IsabelleClient;
import com.veriopt.veritest.isabelle.request.SessionStartRequest;
import com.veriopt.veritest.isabelle.request.SessionStopRequest;
import com.veriopt.veritest.isabelle.request.UseTheoryRequest;
import com.veriopt.veritest.isabelle.response.IsabelleGenericError;
import com.veriopt.veritest.isabelle.response.SessionStartResponse;
import com.veriopt.veritest.isabelle.response.TaskMessage;
import com.veriopt.veritest.isabelle.response.TheoryResponse;
import com.veriopt.veritest.isabelle.utils.Command;
import com.veriopt.veritest.isabelle.utils.TheoryFileTemplate;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Log4j2
@Service
public class IsabelleService {
    private static final String NITPICK_FOUND_STRING = "Nitpick found a counterexample:";
    private static final String SLEDGEHAMMER_FOUND_STRING = "Try this:";
    private static final String SLEDGEHAMMER_DONE = "No proof state";
    public static final String NO_SUBGOAL = "No subgoal!";

    private final @NonNull IsabelleClient client;
    private final @NonNull SessionConfig sessionConfig;

    @Autowired
    public IsabelleService(@NonNull IsabelleClient client, @NonNull SessionConfig sessionConfig) {
        this.client = client;
        this.sessionConfig = sessionConfig;
    }

    public IsabelleResult validateTheory(@Valid @NotNull TheoryRequest request) {
        CompletableFuture<IsabelleResult>[] futures = new CompletableFuture[]{
                tryAutoProof(request),
                tryNitpick(request),
                trySledgehammer(request)
        };

        try {
            // TODO: should change this to return early on success
            // idea: similar to anyOfCancelOthers
            CompletableFuture.allOf(futures).join();
        } catch (CancellationException | CompletionException e) {
            log.error(e.getMessage(), e);

            // TODO: differentiate between failed and errors
            return IsabelleResult.builder()
                        .requestID(request.getRequestId())
                        .status(Status.FAILED)
                        .message(e.getMessage())
                        .build();
        }

        IsabelleResult failedResult = IsabelleResult.builder()
                .requestID(request.getRequestId())
                .status(Status.FAILED)
                .build();

        return Arrays.stream(futures)
                .map(CompletableFuture::resultNow)
                .filter(result -> !Status.FAILED.equals(result.getStatus()))
                .findFirst()
                .orElse(failedResult);
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

    private CompletableFuture<TheoryResponse> submitTheory(String generatedTheory) {
        record TheorySubmission(File file, SessionStartResponse response) { }

        return this.client.startSession(new SessionStartRequest(this.sessionConfig))
                .thenApplyAsync(task -> switch (task) {
                    case IsabelleGenericError error -> {
                        log.error("Session start error: {} | {}", error.getKind(), error.getMessage());

                        // TODO: handle exception
                        throw new RuntimeException(error.getMessage());
                    }
                    case SessionStartResponse startResponse -> startResponse;
                    default -> throw new UnsupportedOperationException();
                })
                .thenApplyAsync(response -> {
                    Path tempDir = Path.of("\\\\wsl$\\Ubuntu"); // TODO: temp fix for windows env
                    tempDir = tempDir.resolve(response.getTempDir());

                    Path filePath = tempDir.resolve(TheoryFileTemplate.theoryFilename());
                    try {
                        File file = generateFile(generatedTheory, filePath);
                        log.debug("Theory: \n{}", generatedTheory);

                        return new TheorySubmission(file, response);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }).thenComposeAsync(theorySubmission -> {
                    final String sessionId = theorySubmission.response().getSessionId();

                    return CompletableFuture.supplyAsync(() -> UseTheoryRequest.builder()
                                    .sessionID(sessionId)
                                    .theories(Collections.singletonList(TheoryFileTemplate.theoryName()))
                                    .build())
                            .thenComposeAsync(client::useTheory)
                            .thenApplyAsync(task -> switch (task) {
                                case TheoryResponse theoryResponse -> theoryResponse;
                                case IsabelleGenericError error -> {
                                    log.error("Isabelle submit theory error | {}: {}", error.getKind(), error.getMessage());

                                    throw new RuntimeException(error.getMessage());
                                }
                                default -> throw new UnsupportedOperationException();
                            })
                            .whenComplete((theoryResponse, throwable) -> {
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
                            });
                });
    }

    private CompletableFuture<IsabelleResult> tryAutoProof(@Valid @NotNull TheoryRequest request) {
        log.debug("Auto Proof for ID = {}", request.getRequestId());

        return CompletableFuture.supplyAsync(() ->
                        TheoryFileTemplate.generate(Command.AUTO, request))
                .thenComposeAsync(this::submitTheory)
                .whenComplete((theoryResponse, ignored) ->
                        log.debug("Get Response for Auto: {}", theoryResponse))
                .thenApplyAsync(theoryResponse -> buildAutoResult(request, theoryResponse));
    }

    private static IsabelleResult buildAutoResult(TheoryRequest request, TheoryResponse theoryResponse) {
        if (!theoryResponse.getOk()) {
            // TODO: log error nodes
            return IsabelleResult.builder()
                    .requestID(request.getRequestId())
                    .status(Status.FAILED)
                    .build();
        }

        return IsabelleResult.builder()
                .requestID(request.getRequestId())
                .status(Status.FOUND_AUTO_PROOF)
                .build();
    }

    private CompletableFuture<IsabelleResult> tryNitpick(@Valid @NotNull TheoryRequest request) {
        log.debug("Nitpick for ID = {}", request.getRequestId());

        return CompletableFuture.supplyAsync(() -> TheoryFileTemplate.generate(Command.NITPICK, request))
                .thenComposeAsync(this::submitTheory)
                .whenComplete((theoryResponse, ignored) ->
                        log.debug("Get Response for Nitpick: {}", theoryResponse))
                .thenApplyAsync(theoryResponse -> buildNitpickResult(request, theoryResponse));
    }

    /* Task Processing */
    private static IsabelleResult buildNitpickResult(TheoryRequest request, TheoryResponse theoryResponse) {
        if (!theoryResponse.getOk()) {
            // TODO: log error nodes
            return IsabelleResult.builder()
                    .requestID(request.getRequestId())
                    .status(Status.FAILED)
                    .build();
        }

        List<TaskMessage> messages = theoryResponse.getNodes().getFirst().getMessages();
        List<String> counterexamples = messages.stream()
                .parallel()
                .map(TaskMessage::getMessage)
                .filter(message -> message.contains(NITPICK_FOUND_STRING))
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
        return CompletableFuture.supplyAsync(() -> TheoryFileTemplate.generate(Command.SLEDGEHAMMER, request))
                .thenComposeAsync(this::submitTheory)
                .whenCompleteAsync((theoryResponse, ignored) -> log.debug("Get Response for Sledgehammer: {}", theoryResponse))
                .thenApplyAsync(theoryResponse -> recursiveSledgehammer(request, theoryResponse));
    }

    private IsabelleResult recursiveSledgehammer(TheoryRequest request, TheoryResponse theoryResponse) {
        /* Base Case: Error in processing */
        if (!theoryResponse.getOk()) {
            // TODO: log error nodes
            return IsabelleResult.builder()
                    .requestID(request.getRequestId())
                    .status(Status.FAILED)
                    .build();
        }

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

        /* Recursion: Try proofs */
        CompletableFuture<IsabelleResult>[] futures = new CompletableFuture[proofs.size()];
        for (int i = 0; i < proofs.size(); i++) {
            TheoryRequest childRequest = TheoryRequest.builder()
                    .requestId(request.getRequestId())
                    .theory(request.getTheory())
                    .build();

            if (request.getProofs() == null) {
                childRequest.setProofs(new ArrayList<>());
            } else {
                childRequest.setProofs(new ArrayList<>(request.getProofs()));
            }

            childRequest.getProofs().add(proofs.get(i));

            futures[i] = trySledgehammer(childRequest);
        }

        // TODO: handle childs which doesn't throw exception
        CompletableFuture.allOf(futures).join();

        /* Recursion end: Propagate successful proofs */
        List<IsabelleResult> results = Arrays.stream(futures)
                .parallel()
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
