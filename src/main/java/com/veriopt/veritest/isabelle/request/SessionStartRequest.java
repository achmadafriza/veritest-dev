package com.veriopt.veritest.isabelle.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.veriopt.veritest.config.SessionConfig;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class SessionStartRequest {
    @NotNull
    private String session;

    @JsonProperty("include_sessions")
    private List<String> includeSessions;

    private String preferences;
    private List<String> options;
    private List<String> dirs;
    private Boolean verbose;

    public SessionStartRequest(SessionConfig config) {
        this.session = config.getSession();
        this.includeSessions = new ArrayList<>(config.getIncludeSessions());
        this.preferences = config.getPreferences();
        this.options = new ArrayList<>(config.getOptions());
        this.dirs = new ArrayList<>(config.getDirs());
        this.verbose = config.getVerbose();
    }
}
