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
    // TODO: either skip this on request, or elaborate for sledgehammer
    private List<String> proofs;
}
