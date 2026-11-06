package com.smarten.module.diagnosis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "规则更新")
public class RuleUpdateRequest {

    @Schema(description = "条件 JSON（白名单字段）")
    private Map<String, Object> condition;

    @Schema(description = "阈值 JSON")
    private Map<String, Object> threshold;

    private Integer priority;

    private Double weight;

    private Boolean enabled;

    private String remark;
}
