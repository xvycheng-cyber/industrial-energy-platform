package com.smarten.module.datalake.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 入库失败记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_ingest_failure")
public class DataIngestFailure extends BaseEntity {

    private String batchNo;

    private Long enterpriseId;

    private Long meterId;

    private LocalDateTime collectTime;

    private String payload;

    private String errorCode;

    private String errorMsg;

    private Integer retryCount;

    /** 0 待处理 / 1 已补录 / 2 已忽略 */
    private Integer status;
}
