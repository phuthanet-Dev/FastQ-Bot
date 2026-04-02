package com.fastq.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the login API response ({@code /loginEmail.ashx}).
 * <p>
 * The critical field is {@code user_token}, which is required as an
 * authentication credential for all subsequent API calls.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {

    @JsonProperty("user_token")
    private String userToken;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("name")
    private String name;
}
