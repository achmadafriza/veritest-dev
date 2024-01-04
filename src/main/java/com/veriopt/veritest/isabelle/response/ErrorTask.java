package com.veriopt.veritest.isabelle.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorTask extends Task{
    private @NonNull String error;
}
