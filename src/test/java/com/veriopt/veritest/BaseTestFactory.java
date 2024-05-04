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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Log4j2
@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
abstract class BaseTestFactory {
    public abstract String getTestName();
    public abstract Path getTestDirectory();
    public abstract int getIterationCount();

    private final List<TestResult> testResults = Collections.synchronizedList(new ArrayList<>());
    private final List<TestResult> summaryResults = Collections.synchronizedList(new ArrayList<>());

    private IsabelleService service;
    private ObjectMapper mapper;

    @BeforeAll
    public void clearTestResults() throws InterruptedException, IOException {
        this.testResults.clear();
        this.summaryResults.clear();

        if (!Files.exists(getTestDirectory())) {
            throw new IllegalArgumentException("Path does not exist!");
        }

        Path resultPath = this.getTestDirectory().resolve("results");
        Files.createDirectories(resultPath);

        ConfigurableApplicationContext context = SpringApplication.run(TestApplication.class);

        this.service = context.getBean(IsabelleService.class);
        this.mapper = context.getBean("isabelleMapper", ObjectMapper.class);

        /* Wait for Isabelle Client to connect */
        Thread.sleep(5000);
    }

    protected Stream<TheoryRequest> loadTestCases() throws IOException {
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

    @TestFactory
    @Execution(ExecutionMode.SAME_THREAD)
    Stream<DynamicTest> runTestCases() throws IOException {
        return loadTestCases().map(test -> DynamicTest.dynamicTest(
                test.getRequestId(),
                () -> {
                    List<TestResult> iterations = new ArrayList<>();
                    for (int i = 1; i <= getIterationCount(); i++) {
                        long startTime = System.currentTimeMillis();
                        IsabelleResult result = service.validateTheory(test);
                        long endTime = System.currentTimeMillis();

                        TestResult testResult = TestResult.builder()
                                .requestId(String.format("%s-%d", test.getRequestId(), i))
                                .resultStatus(result.getStatus())
                                .elapsedTime(endTime - startTime)
                                .result(result.toString())
                                .build();

                        this.testResults.add(testResult);
                        iterations.add(testResult);
                    }

                    long averageElapsedTime = 0;
                    for (TestResult result : iterations) {
                        averageElapsedTime += result.getElapsedTime();
                    }

                    averageElapsedTime = averageElapsedTime / getIterationCount();

                    TestResult summary = TestResult.builder()
                            .requestId(test.getRequestId())
                            .resultStatus(iterations.getFirst().getResultStatus())
                            .elapsedTime(averageElapsedTime)
                            .build();

                    this.summaryResults.add(summary);
                })
        );
    }

    private void generateCsv(List<TestResult> results, List<TestResult> summaryResults) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(TestHeaders.class)
                .build();

        Path resultPath = this.getTestDirectory().resolve("results");
        Path collatedPath = resultPath.resolve(String.format("%s.csv", getTestName()));
        Path summaryPath = resultPath.resolve(String.format("%s-summary.csv", getTestName()));

        try {
            if (!Files.exists(collatedPath)) {
                Files.createFile(collatedPath);
            }

            if (!Files.exists(summaryPath)) {
                Files.createFile(summaryPath);
            }

            StandardOpenOption[] options = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};
            try (final BufferedWriter writer = Files.newBufferedWriter(collatedPath, options);
                 final CSVPrinter printer = new CSVPrinter(writer, format);
                 final BufferedWriter summaryWriter = Files.newBufferedWriter(summaryPath, options);
                 final CSVPrinter summaryPrinter = new CSVPrinter(summaryWriter, format)) {
                for (TestResult result : results) {
                    printer.printRecord(result.getRequestId(), result.getElapsedTime(), result.getResultStatus(), result.getResult());
                }

                for (TestResult result : summaryResults) {
                    summaryPrinter.printRecord(result.getRequestId(), result.getElapsedTime(), result.getResultStatus(), result.getResult());
                }
            }
        } catch (IOException e) {
            log.error("Error occurred after test", e);
            for (TestResult result : results) {
                log.info("Test Result {}: {}", result.getRequestId(), result);
            }

            for (TestResult result : summaryResults) {
                log.info("Summary for {}: {}", result.getRequestId(), result);
            }
        }
    }

    @AfterAll
    public void tearDown() {
        this.testResults.sort(Comparator.comparing(TestResult::getRequestId));
        this.summaryResults.sort(Comparator.comparing(TestResult::getRequestId));

        this.generateCsv(testResults, summaryResults);
    }
}
