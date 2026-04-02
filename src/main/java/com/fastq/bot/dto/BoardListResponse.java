package com.fastq.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardListResponse {

    @JsonProperty("result")
    private Integer result;

    @JsonProperty("result_desc")
    private String resultDesc;

    @JsonProperty("board_list")
    private List<BoardDTO> boardList;

}
