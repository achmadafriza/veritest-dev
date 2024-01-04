package com.veriopt.veritest.isabelle.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

// TODO: unique = nodes
@Data
@EqualsAndHashCode(callSuper = true)
public class TheoryResponse extends Task {
    private Boolean ok;
    private List<TaskMessage> errors;
    private List<TheoryNode> nodes;
}
