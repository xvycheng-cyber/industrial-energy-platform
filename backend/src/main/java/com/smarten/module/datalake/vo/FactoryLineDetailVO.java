package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Schema(description = "三维产线详情")
public class FactoryLineDetailVO {

    private Long id;

    private String code;

    private String name;

    private String workshopName;

    private BigDecimal outputQty;

    private String outputUnit;

    private BigDecimal energyToday;

    private BigDecimal baseline;

    private BigDecimal deviationRate;

    private Integer deviceCount;

    private Integer runningCount;

    private Integer anomalyDeviceCount;

    private Map<String, Integer> riskCounts = new LinkedHashMap<>();

    private boolean baselineAvailable;
}
