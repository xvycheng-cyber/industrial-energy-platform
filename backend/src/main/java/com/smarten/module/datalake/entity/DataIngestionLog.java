package com.smarten.module.datalake.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采集入库批次日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_ingestion_log")
public class DataIngestionLog extends BaseEntity {

    private String batchNo;

    private Long enterpriseId;

    private Long meterId;

    private LocalDate dataDate;

    private String source;

    private Integer expectedCount;

    private Integer receivedCount;

    private Integer validCount;

    private Integer missingCount;

    private BigDecimal successRate;

    /** 0 进行中 / 1 成功 / 2 部分成功 / 3 失败 */
    private Integer status;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
