package com.veriopt.veritest.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@Jacksonized
public class TheoryRequest {
    private String requestId;
    private @NotNull String theory;
    private List<String> proofs;
}
