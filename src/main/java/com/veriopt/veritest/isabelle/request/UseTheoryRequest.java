package com.veriopt.veritest.isabelle.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
public class UseTheoryRequest {
    @NotNull
    @JsonProperty("session_id")
    private String sessionID;

    @NotEmpty
    private List<String> theories;

    // TODO: Specify timeout
//    @PositiveOrZero
//    private int timeout;
}
