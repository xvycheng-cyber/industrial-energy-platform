package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "设备能耗排名")
public class DeviceRankVO {

    private Long deviceId;

    private String deviceName;

    private BigDecimal energyValue;
}
