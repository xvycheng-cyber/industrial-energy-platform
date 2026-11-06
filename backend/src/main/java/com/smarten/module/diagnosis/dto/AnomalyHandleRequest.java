package com.smarten.module.diagnosis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "异常处理")
public class AnomalyHandleRequest {

    /** 1 确认 2 误报 3 转诊断 4 关闭 */
    @jakarta.validation.constraints.NotNull(message = "处理状态不能为空")
    private Integer status;

    private String remark;
}
