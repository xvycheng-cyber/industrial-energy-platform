package com.smarten.module.diagnosis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "诊断规则")
public class DiagnosisRuleVO {

    private Long id;

    private String ruleCode;

    private String ruleName;

    private String anomalyType;

    private Map<String, Object> condition;

    private Map<String, Object> threshold;

    private Double weight;

    private Integer priority;

    private Integer version;

    private Integer enabled;

    private String causeTemplate;

    private String suggestionTemplate;

    private String savingFormula;

    private String remark;
}
