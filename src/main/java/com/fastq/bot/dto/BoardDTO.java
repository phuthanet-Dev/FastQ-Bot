package com.fastq.bot.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
public class BoardDTO {
    @JsonProperty("board_token")
    private String boardToken;

    @JsonProperty("board_name")
    private String boardName;

    @JsonProperty("board_type")
    private int boardType;

    @JsonProperty("box_type_code")
    private String boxTypeCode;

    @JsonProperty("board_location")
    private String boardLocation;

    @JsonProperty("board_picture_url")
    private String boardPictureUrl;

    @JsonProperty("number_of_waiting")
    private int numberOfWaiting;

    private double latitude;
    private double longitude;
    private double distance;

    @JsonProperty("distance_limit")
    private double distanceLimit;

    @JsonProperty("public_flag")
    private int publicFlag;

    @JsonProperty("company_id")
    private int companyId;

    @JsonProperty("happy_time_text")
    private String happyTimeText;

    @JsonProperty("queue_line_list")
    private List<QueueLineDTO> queueLineList; // รายการประเภทคิว

    @JsonProperty("queueing_status")
    private int queueingStatus;

    @JsonProperty("takehome_status")
    private int takehomeStatus;

    @JsonProperty("delivery_status")
    private int deliveryStatus;

    @JsonProperty("privilege_picture_list")
    private List<String> privilegePictureList;

    @JsonProperty("closed_reason")
    private String closedReason;

    @JsonProperty("closed_reason_en")
    private String closedReasonEn;

    @JsonProperty("reserve_flag")
    private int reserveFlag;

    @JsonProperty("reserve_open_time")
    private String reserveOpenTime;

    @JsonProperty("reserve_close_time")
    private String reserveCloseTime;

    @JsonProperty("category_code")
    private String categoryCode;
}