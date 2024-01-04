package com.veriopt.veritest.isabelle.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskMessage {
    private String kind;
    private String message;

    @JsonProperty("pos")
    private Position position;

    private String theory;
    private String session;
    private Integer percentage;

    @Data
    public static class Position {
        private Long id;
        private Integer line;
        private Integer offset;

        @JsonProperty("end_offset")
        private Integer endOffset;

        private String file;
    }
}
