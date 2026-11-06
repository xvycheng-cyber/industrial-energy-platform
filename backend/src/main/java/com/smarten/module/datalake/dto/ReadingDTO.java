package com.smarten.module.datalake.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "单条读数")
public class ReadingDTO {

    @NotNull(message = "仪表 ID 不能为空")
    private Long meterId;

    @NotNull(message = "采集时间不能为空")
    private LocalDateTime collectTime;

    @NotNull(message = "读数不能为空")
    @DecimalMin(value = "0", message = "读数不能为负数")
    private BigDecimal value;

    @Schema(description = "读数类型：1累计读数 2周期增量；为空时取仪表配置")
    private Integer readingType;

    @Schema(description = "点位编码，默认 TOTAL")
    private String pointCode;
}
