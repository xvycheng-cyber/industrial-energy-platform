package com.smarten.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_type")
public class EnergyType extends BaseEntity {

    private String code;

    private String name;

    private String unit;

    private BigDecimal price;

    private BigDecimal co2Factor;

    /** 折标煤系数 kgce/单位 */
    private BigDecimal coalFactor;

    private Integer status;
}
