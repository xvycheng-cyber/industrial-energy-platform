package com.smarten.module.baseline.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "基线训练请求")
public class BaselineTrainRequest {

    @NotNull(message = "维度类型不能为空")
    @Schema(description = "维度：1企业 2车间 3产线 4设备")
    private Integer dimType;

    @NotNull(message = "维度对象不能为空")
    private Long dimId;

    @NotNull(message = "能源类型不能为空")
    private Long energyTypeId;

    @NotNull(message = "训练开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "训练结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "是否训练后立即启用，默认 true")
    private Boolean activate = true;

    @Schema(description = "训练备注")
    private String remark;
}
