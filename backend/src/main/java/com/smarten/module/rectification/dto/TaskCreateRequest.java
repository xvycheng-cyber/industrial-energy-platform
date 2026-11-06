package com.smarten.module.rectification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建整改任务")
public class TaskCreateRequest {

    private Long reportId;

    private Long anomalyId;

    private Long deviceId;

    private String title;

    private String content;

    private String measure;

    private Integer priority = 2;

    private BigDecimal expectedSaving;

    private Long assigneeId;

    private LocalDateTime deadline;
}
