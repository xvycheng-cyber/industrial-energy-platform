package com.smarten.module.datalake.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据缺口（缺失检测结果）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_data_gap")
public class EnergyDataGap extends BaseEntity {

    private Long enterpriseId;

    private Long meterId;

    private Long energyTypeId;

    private LocalDateTime gapStart;

    private LocalDateTime gapEnd;

    private Integer expectedCount;

    private Integer actualCount;

    private Integer missingCount;

    /** 0 待补录 / 1 已补录 / 2 已忽略 */
    private Integer status;

    private LocalDateTime fillTime;

    private String remark;
}
