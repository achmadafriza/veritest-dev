package com.veriopt.veritest.isabelle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veriopt.veritest.isabelle.response.Task;
import com.veriopt.veritest.isabelle.utils.AsyncQueueDTO;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Log4j2
@Component
public class IsabelleProcess extends AbstractIsabelleClient {
    private Lock asyncLock;
    private Condition isEmpty;

    @Autowired
    public IsabelleProcess(@Value("${isabelle.server.name}") @NonNull String serverName,
                           @Value("${isabelle.server.port}") @NonNull String serverPort,
                           @Value("${isabelle.server.password}") @NonNull String password,
                           @Qualifier("isabelleMapper") @NonNull ObjectMapper mapper) {
        super();

        IsabelleProcessInterface client = new IsabelleProcessFacade(mapper);
        this.setClient(client);

        AsyncQueueDTO dto = client.open(serverName, serverPort, password);
        this.setQueue(dto.getAsyncQueue());
        this.asyncLock = dto.getLock();
        this.isEmpty = dto.getIsEmpty();
    }

    @Override
    public void close() throws IOException {
        this.getClient().close();
    }

    // TODO: refactor this
    @Override
    protected <T extends Task> CompletableFuture<T> getElement(String taskID, Class<T> type) {
        log.debug("{} waiting for {}", taskID, type);

        // Source: https://stackoverflow.com/questions/23184964/jdk8-completablefuture-supplyasync-how-to-deal-with-interruptedexception
        CompletableFuture<T> future = new CompletableFuture<>();

        /*
        * Usage of runAsync() starves the ThreadPool as some of getElement() run indefinitely
        * */
        new Thread(new GetElement<T>(this, future, taskID, type)).start();

        return future;
    }

    private static class GetElement<T extends Task> implements Runnable {
        private final IsabelleProcess process;

        private final CompletableFuture<T> future;

        private final String taskID;
        private final Class<T> type;

        GetElement(IsabelleProcess process, CompletableFuture<T> future, String taskID, Class<T> type) {
            this.process = process;
            this.future = future;
            this.taskID = taskID;
            this.type = type;
        }

        @Override
        public void run() {
            while (true) {
                // Await signal on not empty
                this.process.asyncLock.lock();
                try {
                    while (this.process.getQueue().isEmpty()) {
                        try {
                            this.process.isEmpty.await();
                        } catch (InterruptedException e) {
                            future.completeExceptionally(e);
                            Thread.currentThread().interrupt();
                        }
                    }
                } finally {
                    this.process.asyncLock.unlock();
                }

                Task task = this.process.getQueue().peek();
                if (type.isInstance(task) && (taskID.equals(task.getId()))) {
                    log.debug("{} got {}", taskID, type);
                    future.complete(type.cast(this.process.getQueue().remove()));
                    break;
                }
            }
        }
    }
}
