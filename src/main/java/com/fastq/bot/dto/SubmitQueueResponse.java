package com.fastq.bot.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitQueueResponse {

   private int result;

    @JsonProperty("result_desc")
    private String resultDesc;

    @JsonProperty("queue_id")
    private long queueId;

    @JsonProperty("queue_number")
    private String queueNumber;

    @JsonProperty("number_of_waiting")
    private int numberOfWaiting;

    @JsonProperty("queue_datetime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime queueDatetime;

    @JsonProperty("fb_id")
    private String fbId;

    @JsonProperty("wait_seconds")
    private int waitSeconds;

    @JsonProperty("show_wait_seconds_flag")
    private int showWaitSecondsFlag;

    @JsonProperty("time_stamp")
    private String timeStamp;

    @JsonProperty("facebook_image_url")
    private String facebookImageUrl;

    @JsonProperty("coupon_id")
    private int couponId;
}
