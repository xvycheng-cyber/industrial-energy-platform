package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "单设备完整率")
public class DeviceCompletenessVO {

    private Long meterId;

    private String meterCode;

    private String meterName;

    private Long deviceId;

    private String deviceName;

    private Integer expectedCount;

    private Integer validCount;

    private Integer missingCount;

    private BigDecimal completenessRate;
}
