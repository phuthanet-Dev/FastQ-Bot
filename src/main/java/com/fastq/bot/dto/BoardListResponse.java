package com.fastq.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for the board list API response ({@code /reqBoardList.ashx}).
 * <p>
 * Contains the list of queue lines for a target shop and the current
 * queue count per line. Used to determine if the queue conditions are
 * met for booking.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardListResponse {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("line_list")
    private List<QueueLine> lineList;

    /**
     * Represents a single queue line within a shop's board.
     * Each line has its own queue count and ID for submission.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueueLine {

        @JsonProperty("line_id")
        private String lineId;

        @JsonProperty("line_name")
        private String lineName;

        @JsonProperty("current_queue")
        private Integer currentQueue;

        @JsonProperty("max_queue")
        private Integer maxQueue;

        @JsonProperty("is_open")
        private Boolean isOpen;
    }
}
