package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "三维设备详情")
public class FactoryDeviceDetailVO {

    private Long id;

    private String code;

    private String name;

    private String deviceType;

    private String model;

    private BigDecimal ratedPower;

    private Integer status;

    private String statusLabel;

    private String risk;

    private String riskLabel;

    private String riskReason;

    private String lineName;

    private String workshopName;

    private BigDecimal energyToday;

    private BigDecimal baseline;

    private BigDecimal deviationRate;

    private String anomalyLevel;

    private String diagnosis;

    private LocalDateTime updatedAt;

    private boolean baselineAvailable;

    private boolean anomalyAvailable;
}
