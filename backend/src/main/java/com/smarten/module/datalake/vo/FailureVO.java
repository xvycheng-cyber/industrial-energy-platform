package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "入库失败记录")
public class FailureVO {

    private Long id;

    private String batchNo;

    private Long meterId;

    private String meterName;

    private LocalDateTime collectTime;

    private String errorCode;

    private String errorMsg;

    private Integer retryCount;

    private Integer status;

    private String payload;

    private LocalDateTime createTime;
}
