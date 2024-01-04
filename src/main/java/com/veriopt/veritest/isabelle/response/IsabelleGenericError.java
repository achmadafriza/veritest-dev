package com.veriopt.veritest.isabelle.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

// TODO: unique = StopError - {ok, code}
@Data
@EqualsAndHashCode(callSuper = true)
public class IsabelleGenericError extends Task {
    private String kind;
    private String message;
}
