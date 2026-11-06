package com.smarten.module.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_anomaly")
public class EnergyAnomaly extends BaseEntity {

    private Long enterpriseId;

    private Long workshopId;

    private Long lineId;

    private Long deviceId;

    private String anomalyType;

    private Integer severity;

    private BigDecimal riskScore;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private Integer durationMinutes;

    private Integer mergeCount;

    private BigDecimal actualValue;

    private BigDecimal baselineValue;

    private BigDecimal deviationRate;

    private BigDecimal thresholdValue;

    private String ruleCode;

    private String evidenceJson;

    private LocalDateTime detectTime;

    private LocalDateTime firstDetectTime;

    private LocalDateTime lastDetectTime;

    private Long reportId;

    private String detectBatch;

    private Integer status;

    private Long confirmBy;

    private LocalDateTime confirmTime;

    private String remark;
}
