package com.veriopt.veritest.isabelle.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// TODO: unique = none -> default class
@Data
public class Task {
    @JsonProperty("task")
    private String id;

    @JsonIgnore
    private ResultType type;
}
