package com.smarten.module.rectification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "整改反馈")
public class FeedbackRequest {

    private String content;

    private String beforeDesc;

    private String afterDesc;

    private BigDecimal actualSaving;

    private Integer progress;

    private List<String> images = new ArrayList<>();

    /** true 提交完工进入待审核；false 仅进度反馈 */
    private Boolean complete = false;
}
