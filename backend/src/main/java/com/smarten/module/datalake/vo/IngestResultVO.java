package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "批量接入结果")
public class IngestResultVO {

    private String batchNo;

    private int receivedCount;

    private int insertedCount;

    private int duplicatedCount;

    private int failedCount;

    private int expectedCount;

    private BigDecimal completenessRate;

    /** 0 进行中 / 1 成功 / 2 部分成功 / 3 失败 */
    private int status;
}
