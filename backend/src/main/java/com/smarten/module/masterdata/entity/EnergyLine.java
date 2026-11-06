package com.smarten.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_line")
public class EnergyLine extends BaseEntity {

    private Long enterpriseId;

    private Long workshopId;

    private String code;

    private String name;

    private String productType;

    private BigDecimal designCapacity;

    private String capacityUnit;

    private Integer status;

    private String remark;
}
