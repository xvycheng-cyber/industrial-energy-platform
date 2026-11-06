package com.smarten.module.rectification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rectification_task")
public class RectificationTask extends BaseEntity {

    private String taskNo;

    private Long reportId;

    private Long anomalyId;

    private Long enterpriseId;

    private Long lineId;

    private Long deviceId;

    private String title;

    private String content;

    private String measure;

    private Integer priority;

    private BigDecimal expectedSaving;

    private Long assigneeId;

    private String assigneeName;

    private Integer transferCount;

    private LocalDateTime assignTime;

    private LocalDateTime deadline;

    private Integer status;

    private Integer progress;

    private String rejectReason;

    private Integer source;

    private Long auditBy;

    private LocalDateTime auditTime;

    private String baselineSnapshotJson;

    private LocalDateTime finishTime;

    private String remark;
}
