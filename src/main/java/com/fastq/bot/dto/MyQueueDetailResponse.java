package com.fastq.bot.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MyQueueDetailResponse {

    private int result;

    @JsonProperty("result_desc")
    private String resultDesc;

    @JsonProperty("queue_data")
    private QueueData queueData;

    /**
     * Nested object holding the full queue detail payload.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueueData {

        @JsonProperty("queue_id")
        private long queueId;

        @JsonProperty("board_token")
        private String boardToken;

        @JsonProperty("board_name")
        private String boardName;

        @JsonProperty("board_location")
        private String boardLocation;

        @JsonProperty("board_picture_url")
        private String boardPictureUrl;

        @JsonProperty("queue_status")
        private int queueStatus;

        @JsonProperty("queue_number")
        private String queueNumber;

        @JsonProperty("seat_count")
        private int seatCount;

        @JsonProperty("number_of_waiting")
        private int numberOfWaiting;

        @JsonProperty("queue_line_id")
        private int queueLineId;

        @JsonProperty("queue_line_name")
        private String queueLineName;

        @JsonProperty("wait_seconds")
        private int waitSeconds;

        @JsonProperty("show_wait_seconds_flag")
        private int showWaitSecondsFlag;

        @JsonProperty("time_stamp")
        private String timeStamp;

        @JsonProperty("queue_datetime")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime queueDatetime;

        @JsonProperty("facebook_image_url")
        private String facebookImageUrl;

        @JsonProperty("service_list")
        private String serviceList;
    }
}
