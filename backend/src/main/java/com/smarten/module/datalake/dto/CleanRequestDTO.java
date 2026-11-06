package com.smarten.module.datalake.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "清洗触发参数")
public class CleanRequestDTO {

    @Schema(description = "按日期清洗（与 batchNo 二选一）")
    private LocalDate date;

    @Schema(description = "按批次清洗")
    private String batchNo;

    @Schema(description = "单次最大处理条数，默认 5000")
    private Integer limit;
}
