package com.fastq.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {

    private int result;

    @JsonProperty("result_desc")
    private String resultDesc;

    @JsonProperty("user_token")
    private String userToken;
}
