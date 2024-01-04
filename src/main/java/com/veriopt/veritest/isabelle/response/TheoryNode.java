package com.veriopt.veritest.isabelle.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TheoryNode {
    @JsonProperty("node_name")
    private String name;

    @JsonProperty("theory_name")
    private String theoryName;

    private Status status;
    private List<TaskMessage> messages;
    private List<Export> exports;

    @Data
    public static class Status {
        /**
        * ok is an abstraction for failed = 0.
        * */
        private Boolean ok;

        /**
        * Fields total, unprocessed, running, warned, failed, finished account for
         * individual commands within a theory node;
        * */
        private Integer total;
        private Integer unprocessed;
        private Integer running;
        private Integer warned;
        private Integer failed;
        private Integer finished;

        /**
        * The canceled flag tells if some command in the theory has been
         * spontaneously canceled (by an Interrupt exception that could also
         * indicate resource problems).
        * */
        private Boolean canceled;

        /**
        * The consolidated flag indicates whether the outermost theory
         * command structure has finished (or failed) and the final end command has been checked.
        * */
        private Boolean consolidated;

        /**
        * The percentage field tells how far the node has been processed.
         * It ranges between 0 and 99 in normal operation, and reaches 100
         * when the node has been formally consolidated as described above.
        * */
        private Integer percentage;
    }

    @Data
    public static class Export {
        private String name;
        private Boolean base64;
        private String body;
    }
}
