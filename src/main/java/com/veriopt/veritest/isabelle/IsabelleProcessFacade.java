package com.veriopt.veritest.isabelle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.veriopt.veritest.isabelle.config.LimiterConfig;
import com.veriopt.veritest.errors.InstantiationException;
import com.veriopt.veritest.errors.IsabelleException;
import com.veriopt.veritest.isabelle.response.ErrorTask;
import com.veriopt.veritest.isabelle.response.ResultType;
import com.veriopt.veritest.isabelle.response.Task;
import com.veriopt.veritest.isabelle.utils.AsyncQueueDTO;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
@RequiredArgsConstructor
class IsabelleProcessFacade implements IsabelleProcessInterface {
    private final @NonNull String executableLocation;
    private @NonNull ObjectMapper mapper;
    private @NonNull LimiterConfig config;
    private @NonNull Executor ioExecutor;

    private Bucket rateLimiter;
    private Lock syncLock;
    private Process process;
    private BufferedWriter writer;
    private BlockingQueue<Task> syncQueue;

    private Daemon daemon;

    // TODO: need to change this to a TCP client.
    @Override
    public AsyncQueueDTO open(String name, String port, String password) {
        Lock asyncLock = new ReentrantLock();
        AsyncQueueDTO dto = AsyncQueueDTO.builder()
                .asyncQueue(new LinkedBlockingQueue<>())
                .lock(asyncLock)
                .isEmpty(asyncLock.newCondition())
                .build();

        this.syncQueue = new LinkedBlockingQueue<>();
        this.syncLock = new ReentrantLock();

        try {
            this.process = new ProcessBuilder()
//                    .command("wsl.exe", "/mnt/c/Programming/Thesis/Isabelle2023/bin/isabelle", "client", "-n", name, "-p", port)
                    .command(executableLocation, "client", "-n", name, "-p", port)
                    .redirectErrorStream(true)
                    .start();

            BufferedOutputStream outputStream = new BufferedOutputStream(this.process.getOutputStream());
            this.writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            /*
            * TODO: use a faster reader from CP
            *       Note:
            *       - Message format from src/Pure/PIDE/byte_message.scala write_line_message()
            *       - n > 100 || msg.iterator.contains(10)??? have headers
            *       - headers = byte length of the message + new line
            * */
            InputStream inputStream = new BufferedInputStream(process.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            this.daemon = new Daemon(
                    this.process, reader, this.mapper,
                    dto.getAsyncQueue(), dto.getLock(), dto.getIsEmpty(),
                    this.syncQueue
            );

            this.ioExecutor.execute(this.daemon);

            /*
            * Isabelle deadlocks if requests aren't delayed for 2 seconds.
            * Source: https://git.tu-berlin.de/cobalt.rocks/isa-bench
            * From: Joshua Kobschätzki j.k@cobalt.rocks
            * */
            Bandwidth bandwidth = BandwidthBuilder.builder()
                    .capacity(this.config.getToken())
                    .refillIntervally(this.config.getToken(), Duration.ofSeconds(this.config.getPeriod()))
                    .initialTokens(this.config.getToken())
                    .build();

            this.rateLimiter = Bucket.builder()
                    .addLimit(bandwidth)
                    .build();
        } catch (IOException e) {
            log.error("Unable to create IO Pipe from isabelle", e);

            throw new InstantiationException(String.format("%s instantiation error", IsabelleProcessFacade.class), e);
        }

        return dto;
    }

    public CompletableFuture<Void> onExit() {
        return this.process.onExit().thenApply(ignored -> null);
    }

    @Override
    public void close() throws IOException {
        log.info("Closing {}", IsabelleProcessFacade.class);

        this.process.destroy();
        this.writer.close();
        this.daemon.close();
    }

    /**
     * submitTask() returns Task ID.
     * As there can be multiple processes running in IO, the output stream of the client needs
     *  to be copied to avoid data loss on buffer read.
     * Since Task ID response is synchronous, there can only be one process waiting for the Task ID.
     *  As such, it is locked on the Client to avoid clashing Task IDs.
     * If server died, then IO pipe is broken, and IOException is going to occur.
    * */
    @Override
    public String submitTask(TaskType type, Object args) throws InterruptedException {
        this.rateLimiter.asBlocking().consume(1);
        this.syncLock.lockInterruptibly();

        if (!this.process.isAlive()) {
            throw new IsabelleException("Isabelle has died");
        }

        Task response;
        try {
            String request = type + " " + mapper.writeValueAsString(args) + System.lineSeparator();
            writer.write(request);
            writer.flush();
            log.debug("Submit request: {}", request);

            response = this.syncQueue.take();
        } catch (IOException e) {
            throw new IsabelleException("I/O error has occurred", e);
        } finally {
            this.syncLock.unlock();
        }

        return switch (response.getType()) {
            case OK -> response.getId();
            case ERROR -> {
                if (response instanceof ErrorTask error) {
                    log.error("Error on submitting task: {}", error.getError());

                    throw new IsabelleException(error, "Error on submitting task");
                }

                throw new UnsupportedOperationException();
            }
            default -> throw new IllegalStateException("Unexpected value: " + response.getType());
        };
    }

    @Log4j2
    @RequiredArgsConstructor
    private static class Daemon implements Runnable, Closeable {
        private @NonNull Process process;
        private @NonNull BufferedReader reader;

        private @NonNull ObjectMapper mapper;

        private final @NonNull BlockingQueue<Task> asyncQueue;
        private @NonNull Lock asyncLock;
        private @NonNull Condition isEmpty;

        private @NonNull BlockingQueue<Task> syncQueue;

        @Override
        public void close() throws IOException {
            log.info("Closing {}", Daemon.class);

            this.reader.close();
        }

        @Override
        public void run() {
            try {
                while (this.process.isAlive()) {
                    String response = reader.readLine();
                    if (response == null) {
                        log.info("Isabelle process has died");

                        return;
                    }

                    String[] responseArgs = response.trim().split("\\s+", 2);
                    if (responseArgs.length != 2) {
                        if (!isInteger(response)) {
                            log.error("Invalid response: {}", response);
                        }

                        continue;
                    }

                    log.debug("Read: {}", response);

                    parseAndSendTask(responseArgs);
                }
            } catch (IOException e) {
                log.error("IO error occurred", e);
            } catch (InterruptedException e) {
                if (this.process.isAlive()) {
                    log.error("Daemon interrupted", e);

                    Thread.currentThread().interrupt();
                }
            }
        }

        public static boolean isInteger(String s) {
            return isInteger(s,10);
        }

        public static boolean isInteger(String s, int radix) {
            if (s.isEmpty()) {
                return false;
            }

            for(int i = 0; i < s.length(); i++) {
                if (i == 0 && s.charAt(i) == '-') {
                    if(s.length() == 1) {
                        return false;
                    } else {
                        continue;
                    }
                }

                if (Character.digit(s.charAt(i), radix) < 0) {
                    return false;
                }
            }

            return true;
        }


        private void parseAndSendTask(String[] responseArgs) throws InterruptedException {
            try {
                ResultType resultType = ResultType.valueOf(responseArgs[0]);
                Task task;
                switch (resultType) {
                    case FINISHED, FAILED:
                        task = mapper.readValue(responseArgs[1], Task.class);
                        task.setType(resultType);

                        this.asyncLock.lock();
                        try {
                            this.asyncQueue.put(task);
                            this.isEmpty.signalAll();
                        } finally {
                            this.asyncLock.unlock();
                        }
                        break;
                    case OK:
                        task = mapper.readValue(responseArgs[1], Task.class);
                        task.setType(resultType);

                        this.syncQueue.put(task);
                        break;
                    case ERROR:
                        task = new ErrorTask(responseArgs[1]);
                        task.setType(resultType);

                        this.syncQueue.put(task);
                        break;
                    case NOTE:
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            } catch (IllegalArgumentException | MismatchedInputException e) {
                log.debug(e.getMessage(), e);
            } catch (JsonProcessingException e) {
                log.error("Isabelle returned bad JSON {}: {}", responseArgs, e);
            }
        }
    }
}