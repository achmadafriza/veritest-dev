package com.veriopt.veritest.isabelle;

import com.veriopt.veritest.isabelle.request.SessionStartRequest;
import com.veriopt.veritest.isabelle.request.SessionStopRequest;
import com.veriopt.veritest.isabelle.request.UseTheoryRequest;
import com.veriopt.veritest.isabelle.response.Task;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Closeable;

public interface IsabelleClient extends Closeable {
    Task startSession(@Valid @NotNull SessionStartRequest request) throws InterruptedException;
    Task stopSession(SessionStopRequest request) throws InterruptedException;
    Task useTheory(@Valid @NotNull UseTheoryRequest request) throws InterruptedException;
}
