package com.veriopt.veritest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class GraalTest extends BaseTestFactory {
    @Override
    public String getTestName() {
        return "GraalTest";
    }

    @Override
    public Path getTestDirectory() {
        return Paths.get("tests/graal/original");
    }

    @Override
    public int getIterationCount() {
        return 5;
    }

    @Test
    void testCasesLoads() throws IOException {
        Assertions.assertNotEquals(0, this.loadTestCases().count());
    }
}
