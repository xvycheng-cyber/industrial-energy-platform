package com.smarten.module.datalake.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ODS 原始采集数据（只追加，不修改、不物理删除）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_raw_data")
public class EnergyRawData extends BaseEntity {

    private Long enterpriseId;

    private Long meterId;

    private Long deviceId;

    private LocalDateTime collectTime;

    private String pointCode;

    private BigDecimal rawValue;

    private String rawUnit;

    /** 1 累计读数 / 2 周期增量 */
    private Integer readingType;

    private LocalDateTime receivedTime;

    /** 1 正常 / 2 可疑 / 3 无效 */
    private Integer quality;

    private String source;

    private String batchNo;

    /** 0 待清洗 / 1 已清洗 / 2 已忽略 */
    private Integer cleanStatus;

    private String extJson;
}
