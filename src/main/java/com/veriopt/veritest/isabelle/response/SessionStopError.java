package com.veriopt.veritest.isabelle.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

// TODO: unique = none
@Data
@EqualsAndHashCode(callSuper = true)
public class SessionStopError extends Task {
    private Boolean ok;

    @JsonProperty("return_code")
    private Integer code;

    private String kind;
    private String message;
}
