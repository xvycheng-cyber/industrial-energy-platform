package com.smarten.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_device")
public class EnergyDevice extends BaseEntity {

    private Long enterpriseId;

    private Long workshopId;

    private Long lineId;

    private String code;

    private String name;

    private String deviceType;

    private String model;

    private BigDecimal ratedPower;

    private Long energyTypeId;

    private LocalDate commissionDate;

    private Integer status;

    private String remark;
}
