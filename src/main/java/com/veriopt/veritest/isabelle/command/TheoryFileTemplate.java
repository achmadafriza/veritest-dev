package com.veriopt.veritest.isabelle.command;

import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.isabelle.config.TheoryImportConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TheoryFileTemplate {
    private TheoryImportConfig importConfig;

    private final String theoryImports;

    @Autowired
    public TheoryFileTemplate(TheoryImportConfig importConfig) {
        this.importConfig = importConfig;

        StringBuilder builder = new StringBuilder();
        for (String imports : importConfig.getInclude()) {
            builder.append(imports);
            builder.append(System.lineSeparator());
        }

        this.theoryImports = builder.toString();
    }

    public String generate(Command command, TheoryRequest request) {
        StringBuilder proofBuilder = new StringBuilder();
        if (request.getProofs() != null) {
            for (String proof : request.getProofs()) {
                proofBuilder.append(System.lineSeparator());
                proofBuilder.append(proof);
            }
        }

        return """
        theory AutomatedTest
          imports
            Canonicalizations.Common
            Proofs.StampEvalThms
            %s
        begin
                
        phase TemporaryNode
          terminating size
        begin
                
        optimization TemporaryTheory: "%s"%s
        %s
        
        thm_oracles TemporaryTheory
                
        end
                
        end
        """.formatted(this.theoryImports, request.getTheory(), proofBuilder, command);
    }

    public static String theoryFilename() {
        return "AutomatedTest.thy";
    }

    public static String theoryName() {
        return "AutomatedTest";
    }
}
