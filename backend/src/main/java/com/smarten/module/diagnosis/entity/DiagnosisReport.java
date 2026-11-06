package com.smarten.module.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diagnosis_report")
public class DiagnosisReport extends BaseEntity {

    private String reportNo;

    private Long enterpriseId;

    private Long anomalyId;

    private Long deviceId;

    private Long baselineId;

    private String modelVersion;

    private String description;

    private String conclusion;

    private String causeCandidatesJson;

    private String confirmedCause;

    private Long confirmedBy;

    private LocalDateTime confirmedTime;

    private String suggestionsJson;

    private BigDecimal estimatedSaving;

    private String ruleSnapshotJson;

    private Integer status;

    private Integer riskLevel;

    private LocalDateTime generatedAt;
}
