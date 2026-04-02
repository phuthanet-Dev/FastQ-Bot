package com.fastq.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the anti-fraud verification API response ({@code /Queue/Antifraud}).
 * <p>
 * This endpoint validates device integrity by checking the UDID and GPS
 * coordinates against the target shop location. Must pass before submitting a
 * queue.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AntifraudResponse {

    @JsonProperty("return_code")
    private Integer returnCode;

    @JsonProperty("return_message")
    private String returnMessage;
}
