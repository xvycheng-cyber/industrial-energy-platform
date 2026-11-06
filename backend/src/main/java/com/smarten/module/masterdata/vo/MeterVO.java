package com.smarten.module.masterdata.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "计量点")
public class MeterVO {

    private Long id;

    private Long enterpriseId;

    private String enterpriseName;

    private Long deviceId;

    private String deviceName;

    private Long lineId;

    private String lineName;

    private String meterCode;

    private String meterName;

    private Long energyTypeId;

    private String energyTypeName;

    private BigDecimal ratio;

    private LocalDateTime installTime;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
