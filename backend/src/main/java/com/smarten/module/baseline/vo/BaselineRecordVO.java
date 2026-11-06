package com.smarten.module.baseline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "基线明细与偏差")
public class BaselineRecordVO {

    private Long id;

    private Long baselineId;

    private String modelVersion;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private BigDecimal actualValue;

    private BigDecimal baselineValue;

    private BigDecimal deviationValue;

    private BigDecimal deviationRate;

    private Integer baselineType;

    private String baselineTypeName;

    private String fallbackReason;

    private Integer comparable;

    private Map<String, Object> condition;
}
