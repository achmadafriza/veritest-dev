package com.veriopt.veritest.dto;

public enum Status {
    FOUND_COUNTEREXAMPLE,
    FOUND_AUTO_PROOF, FOUND_PROOF,
    MALFORMED, TYPE_ERROR, NO_SUBGOAL,
    SKIPPED_PROOF,
    IN_PROGRESS,
    FAILED,
    ERROR
}
