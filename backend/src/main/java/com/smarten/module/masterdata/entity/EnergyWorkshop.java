package com.smarten.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_workshop")
public class EnergyWorkshop extends BaseEntity {

    private Long enterpriseId;

    private String code;

    private String name;

    private String manager;

    private BigDecimal area;

    private Integer status;

    private String remark;
}
