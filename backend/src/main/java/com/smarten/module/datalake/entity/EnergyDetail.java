package com.smarten.module.datalake.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DWD 能耗明细（清洗后，周期能耗）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_detail")
public class EnergyDetail extends BaseEntity {

    private Long enterpriseId;

    private Long meterId;

    private Long deviceId;

    private Long lineId;

    private Long energyTypeId;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private BigDecimal energyValue;

    private String unit;

    private BigDecimal startReading;

    private BigDecimal endReading;

    /** 是否补录估算 */
    private Integer estimated;

    /** 1 正常 / 2 越界 / 3 缺失补全 / 4 重复 / 5 人工修正 */
    private Integer qualityFlag;

    private Integer sourceCount;

    private String calcVersion;

    private String remark;
}
