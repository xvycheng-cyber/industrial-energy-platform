package com.smarten.module.rectification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "整改任务")
public class RectificationTaskVO {

    private Long id;

    private String taskNo;

    private String title;

    private Long deviceId;

    private String deviceName;

    private String deviceCode;

    private String lineName;

    private Long reportId;

    private Long anomalyId;

    private String ruleName;

    private Integer status;

    private String statusLabel;

    private Integer priority;

    private String priorityLabel;

    private Long assigneeId;

    private String assigneeName;

    private Integer transferCount;

    private BigDecimal expectedSaving;

    private Integer progress;

    private LocalDateTime deadline;

    private boolean overdue;

    private String rejectReason;

    private LocalDateTime assignTime;

    private LocalDateTime finishTime;

    private LocalDateTime auditTime;

    private LocalDateTime createTime;

    private String content;

    private String measure;
}
