package com.veriopt.veritest.isabelle.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

// TODO: unique = {sessionId, tempDir}
@Data
@EqualsAndHashCode(callSuper = true)
public class SessionStartResponse extends Task {
    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("tmp_dir")
    private String tempDir;
}
