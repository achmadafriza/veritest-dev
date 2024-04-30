package com.veriopt.veritest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veriopt.veritest.dto.IsabelleResult;
import com.veriopt.veritest.dto.TestHeaders;
import com.veriopt.veritest.dto.TestResult;
import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.service.IsabelleService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Log4j2
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VerioptTest {
    private static final String testName = "VerioptTest";
    private final List<TestResult> testResults = new ArrayList<>();

    private IsabelleService service;
    private ObjectMapper mapper;

    public Path getTestDirectory() {
        return Paths.get("tests/veriopt/original");
    }

    private Stream<TheoryRequest> loadTestCases() throws IOException {
        return Files.walk(getTestDirectory())
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .peek(log::info)
                .map(path -> {
                    try {
                        byte[] data = Files.readAllBytes(path);

                        return this.mapper.readValue(data, TheoryRequest.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @BeforeAll
    public void clearTestResults() throws InterruptedException {
        this.testResults.clear();

        ConfigurableApplicationContext context = SpringApplication.run(TestApplication.class);

        this.service = context.getBean(IsabelleService.class);
        this.mapper = context.getBean("isabelleMapper", ObjectMapper.class);

        Thread.sleep(5000);
    }

    @TestFactory
    @Execution(ExecutionMode.SAME_THREAD)
    Stream<DynamicTest> runTestCases() throws IOException {
        return loadTestCases().map(test -> DynamicTest.dynamicTest(
                test.getRequestId(),
                () -> {
                    long startTime = System.currentTimeMillis();
                    IsabelleResult result = service.validateTheory(test);
                    long endTime = System.currentTimeMillis();

                    TestResult testResult = TestResult.builder()
                            .requestId(test.getRequestId())
                            .resultStatus(result.getStatus())
                            .elapsedTime(endTime - startTime)
                            .result(result.toString())
                            .build();

                    this.testResults.add(testResult);
                })
        );
    }

    private void generateCsv(List<TestResult> results) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(TestHeaders.class)
                .build();

        StandardOpenOption[] options = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};
        Path filePath = this.getTestDirectory().resolve(String.format("%s.csv", testName));
        try (final BufferedWriter writer = Files.newBufferedWriter(filePath, options);
             final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecords(results);
        } catch (IOException e) {
            log.error("Error occurred after test", e);
            for (TestResult result : results) {
                log.info("Test Result {}: {}", result.getRequestId(), result);
            }
        }
    }

    @AfterAll
    public void tearDown() {
        this.generateCsv(testResults);
    }
}
