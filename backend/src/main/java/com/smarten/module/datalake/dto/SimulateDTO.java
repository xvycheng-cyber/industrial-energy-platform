package com.smarten.module.datalake.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "模拟采集参数（不传则按当前时间生成最近 1 分钟）")
public class SimulateDTO {

    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;

    @Schema(description = "步长（分钟），默认 1")
    private Integer stepMinutes;
}
