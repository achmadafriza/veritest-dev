package com.veriopt.veritest.isabelle.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

// TODO: unique = StopError - {kind, message}
@Data
@EqualsAndHashCode(callSuper = true)
public class SessionStopResponse extends Task {
    private Boolean ok;

    @JsonProperty("return_code")
    private Integer code;
}
