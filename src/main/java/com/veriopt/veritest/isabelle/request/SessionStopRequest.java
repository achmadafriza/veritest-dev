package com.veriopt.veritest.isabelle.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class SessionStopRequest {
    @JsonProperty("session_id")
    private @NonNull String sessionID;
}
