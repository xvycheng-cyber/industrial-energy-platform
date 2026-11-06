package com.smarten.module.baseline.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_baseline")
public class EnergyBaseline extends BaseEntity {

    private Long enterpriseId;

    private Integer dimType;

    private Long dimId;

    private Long energyTypeId;

    private Integer granularity;

    private Integer baselineType;

    private String modelVersion;

    private String algo;

    private Integer sampleCount;

    private LocalDate trainStart;

    private LocalDate trainEnd;

    private BigDecimal r2;

    private BigDecimal mae;

    private BigDecimal rmse;

    private BigDecimal mape;

    private Integer fallbackCount;

    private Integer sampleRejected;

    private Integer outlierCount;

    private String paramsJson;

    private String featureJson;

    private Integer confidence;

    private Integer status;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private LocalDateTime calcTime;
}
