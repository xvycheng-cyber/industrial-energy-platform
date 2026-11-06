package com.smarten.module.baseline.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "基线预测请求")
public class BaselinePredictRequest {

    @Schema(description = "模型 ID，缺省使用当前生效模型")
    private Long baselineId;

    @NotNull(message = "维度类型不能为空")
    private Integer dimType;

    @NotNull(message = "维度对象不能为空")
    private Long dimId;

    @NotNull(message = "能源类型不能为空")
    private Long energyTypeId;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
}
