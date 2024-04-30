package com.veriopt.veritest.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class TestResult {
    private @NonNull String requestId;
    private @NonNull Long elapsedTime;
    private @NonNull Status resultStatus;
    private @NonNull String result;
}
