package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "入库日志")
public class LogVO {

    private Long id;

    private String batchNo;

    private Long enterpriseId;

    private String enterpriseName;

    private LocalDate dataDate;

    private String source;

    private Integer expectedCount;

    private Integer receivedCount;

    private Integer validCount;

    private Integer missingCount;

    private BigDecimal successRate;

    private Integer status;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
