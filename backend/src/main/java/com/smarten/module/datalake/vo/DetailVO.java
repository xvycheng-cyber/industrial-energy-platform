package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "能耗明细")
public class DetailVO {

    private Long id;

    private Long meterId;

    private String meterName;

    private String deviceName;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private BigDecimal energyValue;

    private String unit;

    private BigDecimal startReading;

    private BigDecimal endReading;

    private Integer estimated;

    private Integer qualityFlag;

    private String calcVersion;
}
