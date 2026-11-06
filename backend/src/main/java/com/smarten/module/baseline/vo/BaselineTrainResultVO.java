package com.smarten.module.baseline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "训练结果")
public class BaselineTrainResultVO {

    private Long modelId;

    private String modelVersion;

    private String algo;

    private Integer sampleCount;

    private Integer normalSamples;

    private Integer standbySamples;

    private Integer rejectedSamples;

    private Integer outlierSamples;

    private Integer requiredSamples;

    private BigDecimal mae;

    private BigDecimal rmse;

    private BigDecimal mape;

    private BigDecimal r2;

    private Integer predictedCount;

    private Boolean activated;

    private List<String> fallbacks = new ArrayList<>();
}
