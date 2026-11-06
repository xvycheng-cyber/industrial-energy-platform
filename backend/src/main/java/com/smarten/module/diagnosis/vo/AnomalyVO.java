package com.smarten.module.diagnosis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "异常事件")
public class AnomalyVO {

    private Long id;

    private Long deviceId;

    private String deviceName;

    private String deviceCode;

    private String deviceType;

    private String lineName;

    private String workshopName;

    private String ruleCode;

    private String ruleName;

    private String anomalyType;

    private Integer severity;

    private String riskLabel;

    private BigDecimal riskScore;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private Integer durationMinutes;

    private Integer mergeCount;

    private BigDecimal actualValue;

    private BigDecimal baselineValue;

    private BigDecimal deviationRate;

    private BigDecimal thresholdValue;

    private LocalDateTime detectTime;

    private LocalDateTime lastDetectTime;

    private Integer status;

    private String statusLabel;

    private Long reportId;

    private String reportNo;

    private String remark;

    private Map<String, Object> evidence;
}
