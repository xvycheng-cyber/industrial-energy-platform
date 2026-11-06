package com.smarten.module.masterdata.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "设备")
public class DeviceVO {

    private Long id;

    private Long enterpriseId;

    private String enterpriseName;

    private Long workshopId;

    private String workshopName;

    private Long lineId;

    private String lineName;

    private String code;

    private String name;

    private String deviceType;

    private String model;

    private BigDecimal ratedPower;

    private Long energyTypeId;

    private String energyTypeName;

    private LocalDate commissionDate;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
