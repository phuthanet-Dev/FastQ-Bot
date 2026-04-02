package com.fastq.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the submit queue API response ({@code /submitQueue.ashx}).
 * <p>
 * On success, returns a {@code queue_id} which can be used to track
 * or cancel the booking via {@code /cancelQueue.ashx}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitQueueResponse {

    @JsonProperty("queue_id")
    private String queueId;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("queue_no")
    private String queueNo;
}
