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
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Log4j2
@RequiredArgsConstructor
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractIsabelleClient implements IsabelleClient {
    @Setter(AccessLevel.PROTECTED)
    private IsabelleProcessInterface client;

    @Setter(AccessLevel.PROTECTED)
    private BlockingQueue<Task> queue;

    protected abstract <T extends Task> CompletableFuture<T> getElement(String taskID, Class<T> type);

    /**
     * 1 User -> 1 Session ID
     *        -> n Thread for each fork of Use Theory
     * 1 Thread -> 1 Task ID
    * */
    @Override
    public Task startSession(@Valid @NotNull SessionStartRequest request) throws InterruptedException {
        // TODO: Handle exception
        Task result;
        try {
            String taskId = this.client.submitTask(TaskType.SESSION_START, request);

            result = (Task) CompletableFuture.anyOf(
                    getElement(taskId, SessionStartResponse.class),
                    getElement(taskId, IsabelleGenericError.class)
            ).get();
        } catch (ExecutionException e) {
            // TODO: Handle logging
            log.error("Task failure", e);
            return null;
        } catch (IOException e) {
            // TODO: Handle logging
            log.error("IO failure", e);
            return null;
        }

        return switch (result) {
            case SessionStartResponse response -> response;
            case IsabelleGenericError error -> {
                log.error("Isabelle start session error | {}: {}", error.getKind(), error.getMessage());
                yield error;
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Task stopSession(SessionStopRequest request) throws InterruptedException {
        String taskId;
        try {
            taskId = this.client.submitTask(TaskType.SESSION_STOP, request);

            return CompletableFuture.anyOf(
                    getElement(taskId, SessionStopResponse.class),
                    getElement(taskId, SessionStopError.class)
            ).thenApply(result -> switch (result) {
                case SessionStopResponse response -> response;
                case SessionStopError error -> {
                    // TODO: throw exception
                    log.error("Isabelle stop session error | {}: {}", error.getKind(), error.getMessage());
                    yield error;
                }
                default -> throw new UnsupportedOperationException();
            }).get();
        } catch (ExecutionException e) {
            // TODO: Handle exception
            // TODO: Handle logging
            log.error("Task failure", e);
            return null;
        } catch (IOException e) {
            // TODO: Handle exception
            // TODO: Handle logging
            log.error("IO failure", e);
            return null;
        }
    }

    @Override
    public CompletableFuture<Task> useTheory(@Valid @NotNull UseTheoryRequest request) throws InterruptedException {
        String taskId;
        try {
            taskId = this.client.submitTask(TaskType.USE_THEORIES, request);
        }  catch (IOException e) {
            // TODO: Handle exception
            // TODO: Handle logging
            log.error("IO failure", e);
            return null;
        }

        return CompletableFuture.anyOf(
                getElement(taskId, TheoryResponse.class),
                getElement(taskId, IsabelleGenericError.class)
        ).thenApply(result -> switch (result) {
            case TheoryResponse response -> response;
            case IsabelleGenericError error -> {
                // TODO: throw exception
                log.error("Isabelle submit theory error | {}: {}", error.getKind(), error.getMessage());
                yield error;
            }
            default -> throw new UnsupportedOperationException();
        });
    }
}
