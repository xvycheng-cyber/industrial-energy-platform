package com.smarten.module.datalake.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DWS 能耗聚合。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_aggregate")
public class EnergyAggregate extends BaseEntity {

    private Long enterpriseId;

    /** 1 企业 / 2 车间 / 3 产线 / 4 设备 */
    private Integer dimType;

    private Long dimId;

    private Long energyTypeId;

    /** 1 小时 / 2 日 / 3 月 / 4 5分钟 / 5 15分钟 */
    private Integer periodType;

    private LocalDateTime periodStart;

    private BigDecimal energyValue;

    private BigDecimal costAmount;

    private Integer dataCount;

    private Integer missingCount;

    private String calcVersion;
}
