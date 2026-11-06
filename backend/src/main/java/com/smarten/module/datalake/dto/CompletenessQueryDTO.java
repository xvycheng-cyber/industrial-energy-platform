package com.smarten.module.datalake.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "完整率查询参数")
public class CompletenessQueryDTO {

    @Schema(description = "统计日期，默认今天")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @Schema(description = "企业 ID，非平台管理员忽略")
    private Long enterpriseId;

    @Schema(description = "设备 ID")
    private Long deviceId;

    @Schema(description = "仪表 ID")
    private Long meterId;
}
