package com.veriopt.veritest.isabelle;

import com.veriopt.veritest.isabelle.utils.AsyncQueueDTO;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

interface IsabelleProcessInterface extends Closeable {
    AsyncQueueDTO open(String name, String port, String password);

    String submitTask(TaskType type, Object args) throws InterruptedException;

    CompletableFuture<Void> onExit();
}
