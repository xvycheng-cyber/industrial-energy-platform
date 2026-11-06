package com.smarten.module.datalake.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量接入参数")
public class IngestBatchDTO {

    @NotBlank(message = "批次号不能为空")
    @Size(max = 64)
    @Schema(description = "批次号，幂等键", example = "BATCH-20261104-001")
    private String batchNo;

    @NotBlank(message = "数据来源不能为空")
    @Schema(description = "GATEWAY / IMPORT / MANUAL / API / SIMULATOR", example = "API")
    private String source;

    @NotEmpty(message = "读数不能为空")
    @Size(max = 2000, message = "单批次最多 2000 条")
    @Valid
    @Schema(description = "读数列表")
    private List<ReadingDTO> readings;
}
