package com.veriopt.veritest.isabelle.command;

import com.veriopt.veritest.dto.TheoryRequest;

public class TheoryFileTemplate {
    public static String generate(Command command, TheoryRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.getProofs() != null) {
            for (String proof : request.getProofs()) {
                builder.append(System.lineSeparator());
                builder.append(proof);
            }
        }

        return """
        theory AutomatedTest
          imports
            Canonicalizations.Common
            Proofs.StampEvalThms
        begin
                
        phase TemporaryNode
          terminating size
        begin
                
        optimization TemporaryTheory: "%s"%s
        %s
                
        end
                
        end
        """.formatted(request.getTheory(), builder, command);
    }

    public static String theoryFilename() {
        return "AutomatedTest.thy";
    }

    public static String theoryName() {
        return "AutomatedTest";
    }
}
