package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "原始数据")
public class RawDataVO {

    private Long id;

    private Long meterId;

    private String meterCode;

    private String meterName;

    private String deviceName;

    private LocalDateTime collectTime;

    private BigDecimal rawValue;

    private String rawUnit;

    private Integer readingType;

    private Integer quality;

    private String source;

    private String batchNo;

    private Integer cleanStatus;

    private LocalDateTime receivedTime;
}
