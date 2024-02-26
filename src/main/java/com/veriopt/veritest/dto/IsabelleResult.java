package com.veriopt.veritest.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@Builder
public class IsabelleResult {
    private @NonNull String requestID;
    private @NonNull Status status;

    private String message;
    private String counterexample;
    private List<String> proofs;
    private List<String> isabelleMessages;
}
