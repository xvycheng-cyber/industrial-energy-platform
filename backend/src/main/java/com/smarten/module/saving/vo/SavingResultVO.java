package com.smarten.module.saving.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "节能核算结果")
public class SavingResultVO {

    private Long id;

    private Long taskId;

    private String taskNo;

    private String title;

    private Long deviceId;

    private String deviceName;

    private String lineName;

    private Integer version;

    private LocalDateTime verifyStart;

    private LocalDateTime verifyEnd;

    private Integer actualDays;

    private Integer sampleDays;

    private String baselineModelVersion;

    private BigDecimal baselineTotal;

    private BigDecimal actualTotal;

    private BigDecimal savingValue;

    private BigDecimal savingRate;

    private BigDecimal savingAmount;

    private BigDecimal unitPrice;

    private BigDecimal completeness;

    private Integer comparable;

    private BigDecimal outputBefore;

    private BigDecimal outputAfter;

    private BigDecimal outputChangeRate;

    private Integer conclusionCode;

    private String conclusion;

    private String reason;

    private Integer status;

    private String statusLabel;

    private Integer archived;

    private LocalDateTime auditedAt;

    private String auditRemark;

    private LocalDateTime createTime;

    private List<PointVO> points = new ArrayList<>();

    private List<VersionVO> versions = new ArrayList<>();

    @Data
    public static class PointVO {
        private LocalDateTime time;
        private BigDecimal baseline;
        private BigDecimal actual;
        private BigDecimal saving;
        private BigDecimal price;
    }

    @Data
    public static class VersionVO {
        private Long id;
        private Integer version;
        private String conclusion;
        private BigDecimal savingValue;
        private LocalDateTime createTime;
        private Integer archived;
    }
}
