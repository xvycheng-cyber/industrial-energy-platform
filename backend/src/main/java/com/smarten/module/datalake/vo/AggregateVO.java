package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "聚合结果")
public class AggregateVO {

    private Long id;

    private Integer dimType;

    private Long dimId;

    private String dimName;

    private Long energyTypeId;

    private String energyTypeName;

    private String unit;

    private Integer periodType;

    private LocalDateTime periodStart;

    private BigDecimal energyValue;

    private BigDecimal costAmount;

    private Integer dataCount;
}
