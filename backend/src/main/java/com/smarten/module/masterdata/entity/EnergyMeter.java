package com.smarten.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_meter")
public class EnergyMeter extends BaseEntity {

    private Long enterpriseId;

    private Long deviceId;

    private Long lineId;

    private String meterCode;

    private String meterName;

    private Long energyTypeId;

    private BigDecimal ratio;

    /** 采集间隔（秒） */
    private Integer collectIntervalSeconds;

    /** 读数类型：1累计读数 2周期增量 */
    private Integer readingType;

    private LocalDateTime installTime;

    private Integer status;

    private String remark;
}
