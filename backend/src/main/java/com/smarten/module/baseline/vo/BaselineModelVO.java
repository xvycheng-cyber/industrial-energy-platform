package com.smarten.module.baseline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "基线模型")
public class BaselineModelVO {

    private Long id;

    private Long enterpriseId;

    private Integer dimType;

    private Long dimId;

    private String dimName;

    private Long energyTypeId;

    private String energyTypeName;

    private String modelVersion;

    private String algo;

    private Integer baselineType;

    private Integer sampleCount;

    private Integer standbySamples;

    private Integer sampleRejected;

    private Integer outlierCount;

    private Integer fallbackCount;

    private BigDecimal mae;

    private BigDecimal rmse;

    private BigDecimal mape;

    private BigDecimal r2;

    private Integer confidence;

    private Integer status;

    private String statusLabel;

    private LocalDate trainStart;

    private LocalDate trainEnd;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private LocalDateTime calcTime;

    private List<String> featureNames = new ArrayList<>();

    private List<String> fallbackLog = new ArrayList<>();

    private Map<String, Object> params;
}
