package com.smarten.module.diagnosis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "诊断报告")
public class DiagnosisReportVO {

    private Long id;

    private String reportNo;

    private Long anomalyId;

    private Long deviceId;

    private String deviceName;

    private String deviceCode;

    private String ruleCode;

    private String ruleName;

    private String description;

    private String conclusion;

    private List<String> causes = new ArrayList<>();

    private String confirmedCause;

    private LocalDateTime confirmedTime;

    private List<String> suggestions = new ArrayList<>();

    private BigDecimal estimatedSaving;

    private Integer riskLevel;

    private String riskLabel;

    private Integer status;

    private LocalDateTime generatedAt;

    private BigDecimal actualValue;

    private BigDecimal baselineValue;

    private BigDecimal deviationRate;

    private Map<String, Object> ruleSnapshot;
}
