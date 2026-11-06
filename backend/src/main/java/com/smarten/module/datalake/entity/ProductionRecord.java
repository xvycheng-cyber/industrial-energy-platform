package com.smarten.module.datalake.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产工况（当前由模拟采集生成，P5 基线使用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_record")
public class ProductionRecord extends BaseEntity {

    private Long enterpriseId;

    private Long workshopId;

    private Long lineId;

    private Long deviceId;

    private LocalDate recordDate;

    /** 1 白 / 2 中 / 3 夜 */
    private Integer shift;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String productType;

    private BigDecimal outputQty;

    private String outputUnit;

    private Integer runMinutes;

    private BigDecimal loadRate;

    private BigDecimal ambientTemp;

    private Integer status;

    private String dataSource;
}
