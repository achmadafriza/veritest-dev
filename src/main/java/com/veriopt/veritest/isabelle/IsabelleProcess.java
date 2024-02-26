package com.veriopt.veritest.isabelle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veriopt.veritest.config.LimiterConfig;
import com.veriopt.veritest.isabelle.response.Task;
import com.veriopt.veritest.isabelle.utils.AsyncQueueDTO;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Log4j2
@Component
public class IsabelleProcess extends AbstractIsabelleClient {
    private final Lock asyncLock;
    private final Condition isEmpty;

    @Autowired
    public IsabelleProcess(@Value("${isabelle.server.name}") @NonNull String serverName,
                           @Value("${isabelle.server.port}") @NonNull String serverPort,
                           @Value("${isabelle.server.password}") @NonNull String password,
                           @NonNull LimiterConfig config,
                           @Qualifier("isabelleMapper") @NonNull ObjectMapper mapper,
                           ApplicationContext context) {
        super();

        IsabelleProcessInterface client = new IsabelleProcessFacade(mapper, config);
        this.setClient(client);

        AsyncQueueDTO dto = client.open(serverName, serverPort, password);
        this.setQueue(dto.getAsyncQueue());
        this.asyncLock = dto.getLock();
        this.isEmpty = dto.getIsEmpty();

        client.onExit().thenRun(() -> SpringApplication.exit(context));
    }

    @Override
    public void close() throws IOException {
        log.info("Closing {}", IsabelleProcess.class);

        this.getClient().close();
    }

    @Override
    protected <T extends Task> CompletableFuture<T> getElement(String taskID, Class<T> type) {
        log.debug("{} waiting for {}", taskID, type);

        // Source: https://stackoverflow.com/questions/23184964/jdk8-completablefuture-supplyasync-how-to-deal-with-interruptedexception
        CompletableFuture<T> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            while (Future.State.RUNNING.equals(future.state())) {
                // Await signal on not empty
                asyncLock.lock();
                try {
                    while (getQueue().isEmpty()) {
                        try {
                            isEmpty.await();
                        } catch (InterruptedException e) {
                            future.completeExceptionally(e);
                            Thread.currentThread().interrupt();
                        }
                    }

                    Task task = getQueue().peek();
                    if (type.isInstance(task) && (taskID.equals(task.getId()))) {
                        log.debug("{} got {}", taskID, type);

                        // Only one thread will have the corresponding Task
                        future.complete(type.cast(getQueue().remove()));
                        break;
                    }
                } finally {
                    asyncLock.unlock();
                }
            }
        });

        return future;
    }
}
