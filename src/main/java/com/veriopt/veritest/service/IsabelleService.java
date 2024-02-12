package com.veriopt.veritest.service;

import com.veriopt.veritest.config.SessionConfig;
import com.veriopt.veritest.dto.IsabelleResult;
import com.veriopt.veritest.dto.Status;
import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.isabelle.IsabelleClient;
import com.veriopt.veritest.isabelle.request.SessionStartRequest;
import com.veriopt.veritest.isabelle.request.SessionStopRequest;
import com.veriopt.veritest.isabelle.request.UseTheoryRequest;
import com.veriopt.veritest.isabelle.response.*;
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
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
            CompletableFuture.allOf(futures).join();
        } catch (CancellationException | CompletionException e) {
            log.error(e.getMessage(), e);

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

    private TheoryResponse submitTheory(String generatedTheory) {
        Task sessionTask;
        try {
            sessionTask = this.client.startSession(new SessionStartRequest(this.sessionConfig));
        } catch (InterruptedException e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }

        String sessionId;
        Path tempDir = Path.of("\\\\wsl$\\Ubuntu"); // TODO: temp fix for windows env
        switch (sessionTask) {
            case IsabelleGenericError error -> {
                log.error("Session start error: {} | {}", error.getKind(), error.getMessage());

                // TODO: handle exception
                throw new RuntimeException(error.getMessage());
            }
            case SessionStartResponse response -> {
                sessionId = response.getSessionId();
                tempDir = tempDir.resolve(response.getTempDir());
            }
            default -> throw new UnsupportedOperationException();
        }

        Path tempFile = tempDir.resolve(TheoryFileTemplate.theoryFilename());
        File file = tempFile.toFile();

        TheoryResponse theoryResponse;
        try {
            if (file.exists() || !file.createNewFile()) {
                throw new IOException("File Cannot be created");
            }

            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            PrintWriter writer = new PrintWriter(fileWriter);

            writer.print(generatedTheory);
            writer.close();

            log.debug("File exists: {}", file.exists());
            log.debug("Theory: \n{}", generatedTheory);

            UseTheoryRequest useTheoryRequest = UseTheoryRequest.builder()
                    .sessionID(sessionId)
                    .theories(Collections.singletonList(TheoryFileTemplate.theoryName()))
                    .build();

            Task taskResult = client.useTheory(useTheoryRequest);

            // TODO: handle exceptions
            theoryResponse = switch (taskResult) {
                case TheoryResponse response -> response;
                case IsabelleGenericError error -> throw new RuntimeException(error.getMessage());
                default -> throw new UnsupportedOperationException();
            };
        } catch (InterruptedException | IOException e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        } finally {
            boolean ignored = file.delete();

            SessionStopRequest stopRequest = SessionStopRequest.builder()
                    .sessionID(sessionId)
                    .build();

            // TODO: fix this
            Thread stopThread = new Thread(() -> {
                try {
                    client.stopSession(stopRequest);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            stopThread.start();
        }

        return theoryResponse;
    }

    private CompletableFuture<IsabelleResult> tryAutoProof(@Valid @NotNull TheoryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Auto Proof for ID = {}", request.getRequestId());
            String generatedTheory = TheoryFileTemplate.generate(Command.AUTO, request);
            TheoryResponse theoryResponse = submitTheory(generatedTheory);

            log.debug("Get Response for Auto: {}", theoryResponse);

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
        });
    }

    private CompletableFuture<IsabelleResult> tryNitpick(@Valid @NotNull TheoryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Nitpick for ID = {}", request.getRequestId());
            String generatedTheory = TheoryFileTemplate.generate(Command.NITPICK, request);
            TheoryResponse theoryResponse = submitTheory(generatedTheory);

            log.debug("Get Response for Nitpick: {}", theoryResponse);

            /* Task Processing */
            if (!theoryResponse.getOk()) {
                // TODO: log error nodes
                return IsabelleResult.builder()
                                .requestID(request.getRequestId())
                                .status(Status.FAILED)
                                .build();
            }

            List<TaskMessage> messages = theoryResponse.getNodes().get(0).getMessages();
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
                                .counterexample(counterexamples.get(0))
                                .build();
        });
    }

    /*
    * Done -> No proof state
    * In Progress -> ___: Try this: {proof}
    * No Proof -> No try this and not done
    * */
    private CompletableFuture<IsabelleResult> trySledgehammer(@Valid @NotNull TheoryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Sledgehammer for ID = {}", request.getRequestId());
            String generatedTheory = TheoryFileTemplate.generate(Command.SLEDGEHAMMER, request);
            TheoryResponse theoryResponse = submitTheory(generatedTheory);

            log.debug("Get Response for Sledgehammer: {}", theoryResponse);

            /* Base Case: Error in processing */
            if (!theoryResponse.getOk()) {
                // TODO: log error nodes
                return IsabelleResult.builder()
                                .requestID(request.getRequestId())
                                .status(Status.FAILED)
                                .build();
            }

            List<TaskMessage> messages = theoryResponse.getNodes().get(0).getMessages();

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

            return results.get(0);
        });
    }
}
