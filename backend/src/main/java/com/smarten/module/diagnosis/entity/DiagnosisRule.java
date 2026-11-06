package com.smarten.module.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diagnosis_rule")
public class DiagnosisRule extends BaseEntity {

    private String ruleCode;

    private String ruleName;

    private String anomalyType;

    private String conditionJson;

    private String thresholdJson;

    private BigDecimal weight;

    private String causeTemplate;

    private String suggestionTemplate;

    private String savingFormula;

    private Integer priority;

    private Integer version;

    private Integer enabled;

    private Long enterpriseId;

    private String remark;
}
