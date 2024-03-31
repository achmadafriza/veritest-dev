package com.veriopt.veritest.isabelle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties("isabelle.theories")
public class TheoryImportConfig {
    private List<String> include;
}
