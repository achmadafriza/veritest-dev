package com.veriopt.veritest.isabelle;

import com.veriopt.veritest.isabelle.request.SessionStartRequest;
import com.veriopt.veritest.isabelle.request.SessionStopRequest;
import com.veriopt.veritest.isabelle.request.UseTheoryRequest;
import com.veriopt.veritest.isabelle.response.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

@Log4j2
@RequiredArgsConstructor
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractIsabelleClient implements IsabelleClient {
    @Setter(AccessLevel.PROTECTED)
    private IsabelleProcessInterface client;

    @Setter(AccessLevel.PROTECTED)
    private BlockingQueue<Task> queue;

    protected abstract <T extends Task> CompletableFuture<T> getResult(String taskID, Class<T> type);

    protected CompletableFuture<Object> anyOfCancelOthers(CompletableFuture<?>... futures) {
        final CompletableFuture<Object> futureResult = CompletableFuture.anyOf(futures);
        futureResult.thenRun(() -> Arrays.stream(futures)
                .filter(completableFuture -> !completableFuture.isDone())
                .forEach(completableFuture -> completableFuture.cancel(true)));

        return futureResult;
    }

    /**
     * 1 User -> 1 Session ID
     *        -> n Thread for each fork of Use Theory
     * 1 Thread -> 1 Task ID
    * */
    @Override
    public CompletableFuture<Task> startSession(@Valid @NotNull SessionStartRequest request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                future.complete(this.client.submitTask(TaskType.SESSION_START, request));
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        });

        return future.thenCompose(taskId -> anyOfCancelOthers(
                getResult(taskId, SessionStartResponse.class),
                getResult(taskId, IsabelleGenericError.class)
        )).thenApplyAsync(result -> switch (result) {
            case SessionStartResponse response -> response;
            case IsabelleGenericError error -> {
                log.error("Isabelle start session error | {}: {}", error.getKind(), error.getMessage());

                yield error;
            }
            default -> throw new IllegalStateException("Unexpected value: " + result);
        });
    }

    @Override
    public CompletableFuture<Task> stopSession(SessionStopRequest request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                future.complete(this.client.submitTask(TaskType.SESSION_STOP, request));
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        });

        return future.thenCompose(taskId -> anyOfCancelOthers(
                getResult(taskId, SessionStopResponse.class),
                getResult(taskId, SessionStopError.class)
        )).thenApplyAsync(result -> switch (result) {
            case SessionStopResponse response -> response;
            case SessionStopError error -> {
                log.error("Isabelle stop session error | {}: {}", error.getKind(), error.getMessage());

                yield error;
            }
            default -> throw new IllegalStateException("Unexpected value: " + result);
        });
    }

    @Override
    public CompletableFuture<Task> useTheory(@Valid @NotNull UseTheoryRequest request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                future.complete(this.client.submitTask(TaskType.USE_THEORIES, request));
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        });

        return future.thenCompose(taskId -> anyOfCancelOthers(
                getResult(taskId, TheoryResponse.class),
                getResult(taskId, IsabelleGenericError.class)
        )).thenApplyAsync(result -> switch (result) {
            case TheoryResponse response -> response;
            case IsabelleGenericError error -> {
                log.error("Isabelle use theory error | {}: {}", error.getKind(), error.getMessage());

                yield error;
            }
            default -> throw new IllegalStateException("Unexpected value: " + result);
        });
    }
}
