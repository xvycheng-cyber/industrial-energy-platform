package com.smarten.module.rectification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "整改反馈")
public class RectificationFeedbackVO {

    private Long id;

    private Integer feedbackType;

    private String feedbackTypeLabel;

    private String content;

    private String beforeDesc;

    private String afterDesc;

    private List<String> images = new ArrayList<>();

    private BigDecimal actualSaving;

    private Long submitBy;

    private String submitByName;

    private LocalDateTime submitTime;

    private Integer verifyResult;

    private String verifyRemark;

    private LocalDateTime verifyTime;
}
