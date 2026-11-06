package com.smarten.module.rectification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "任务审核/效果验证")
public class TaskAuditRequest {

    private Boolean pass;

    private String reason;

    private java.math.BigDecimal actualSaving;

    private String remark;
}
