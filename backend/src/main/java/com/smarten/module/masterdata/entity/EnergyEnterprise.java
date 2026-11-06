package com.smarten.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_enterprise")
public class EnergyEnterprise extends BaseEntity {

    private String code;

    private String name;

    private String industry;

    private String region;

    private String address;

    private String contactName;

    private String contactPhone;

    private Integer status;

    private String remark;
}
