package com.veriopt.veritest.isabelle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("isabelle.rate-limit")
public class LimiterConfig {
    private long token;
    private long period;
}
