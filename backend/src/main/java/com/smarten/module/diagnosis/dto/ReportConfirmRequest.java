package com.smarten.module.diagnosis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "人工确认故障原因")
public class ReportConfirmRequest {

    @NotBlank(message = "确认原因不能为空")
    private String confirmedCause;
}
