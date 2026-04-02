package com.fastq.bot.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class QueueLineDTO {
    @JsonProperty("queue_line_id")
    private int queueLineId;

    @JsonProperty("queue_line_type_no")
    private int queueLineTypeNo;

    @JsonProperty("queue_line_name")
    private String queueLineName;

    @JsonProperty("seat_count_flag")
    private int seatCountFlag;

    @JsonProperty("seat_count_min")
    private int seatCountMin;

    @JsonProperty("seat_count_max")
    private int seatCountMax;
}
