package com.smarten.module.rectification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "整改任务详情")
public class RectificationTaskDetailVO extends RectificationTaskVO {

    private List<RectificationFeedbackVO> feedbacks = new ArrayList<>();

    private String reportDescription;

    private String reportConclusion;

    private String reportNo;

    private List<String> anomalies = new ArrayList<>();
}
