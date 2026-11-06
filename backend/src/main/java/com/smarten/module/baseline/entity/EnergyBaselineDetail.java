package com.smarten.module.baseline.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_baseline_detail")
public class EnergyBaselineDetail extends BaseEntity {

    private Long baselineId;

    private Long enterpriseId;

    private Integer dimType;

    private Long dimId;

    private Long energyTypeId;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private BigDecimal baselineValue;

    private BigDecimal actualValue;

    private BigDecimal deviationValue;

    private BigDecimal deviationRate;

    private Integer baselineType;

    private String modelVersion;

    private String conditionJson;

    private String fallbackReason;

    private Integer estimated;

    private Integer comparable;

    private LocalDateTime computedAt;
}
