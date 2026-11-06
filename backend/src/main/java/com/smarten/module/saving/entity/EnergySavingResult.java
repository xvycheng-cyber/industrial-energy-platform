package com.smarten.module.saving.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_saving_result")
public class EnergySavingResult extends BaseEntity {

    private Long taskId;

    private Long reportId;

    private Long enterpriseId;

    private Long deviceId;

    private Integer version;

    private LocalDateTime verifyStart;

    private LocalDateTime verifyEnd;

    private Integer sampleDays;

    private Integer actualDays;

    private Integer method;

    private String baselineModelVersion;

    private BigDecimal baselineTotal;

    private BigDecimal actualTotal;

    private BigDecimal savingValue;

    private BigDecimal savingRate;

    private BigDecimal savingAmount;

    private String conclusion;

    private BigDecimal completeness;

    private Integer comparable;

    private BigDecimal outputBefore;

    private BigDecimal outputAfter;

    private BigDecimal outputChangeRate;

    private BigDecimal unitPrice;

    private Integer confidence;

    private String excludedJson;

    private String detailJson;

    private Integer status;

    private Integer archived;

    private Long auditedBy;

    private LocalDateTime auditedAt;

    private String auditRemark;
}
