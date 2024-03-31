package com.veriopt.veritest.isabelle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties("isabelle.session")
public class SessionConfig {
    private String session;
    private List<String> includeSessions;
    private String preferences;
    private List<String> options;
    private List<String> dirs;
    private Boolean verbose;
}
