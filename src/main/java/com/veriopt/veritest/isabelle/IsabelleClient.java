package com.veriopt.veritest.isabelle;

import com.veriopt.veritest.isabelle.request.SessionStartRequest;
import com.veriopt.veritest.isabelle.request.SessionStopRequest;
import com.veriopt.veritest.isabelle.request.UseTheoryRequest;
import com.veriopt.veritest.isabelle.response.Task;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface IsabelleClient extends Closeable {
    CompletableFuture<Task> startSession(@Valid @NotNull SessionStartRequest request);
    CompletableFuture<Task> stopSession(SessionStopRequest request);
    CompletableFuture<Task> useTheory(@Valid @NotNull UseTheoryRequest request);
}
