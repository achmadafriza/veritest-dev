package com.veriopt.veritest.isabelle.utils;

import com.veriopt.veritest.isabelle.response.Task;
import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Getter
@Builder
public class AsyncQueueDTO {
    private Lock lock;
    private Condition isEmpty;
    private BlockingQueue<Task> asyncQueue;
}
