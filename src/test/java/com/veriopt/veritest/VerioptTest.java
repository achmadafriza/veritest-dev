package com.veriopt.veritest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerioptTest extends BaseTestFactory {
    @Override
    public String getTestName() {
        return "VerioptTest";
    }

    @Override
    public Path getTestDirectory() {
        return Paths.get("tests/veriopt/original");
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
