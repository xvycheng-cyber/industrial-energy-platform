package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "产线能耗画像")
public class LineProfileVO {

    private Long id;

    private String code;

    private String name;

    private String enterpriseName;

    private String workshopName;

    private Integer days;

    private BigDecimal energy;

    private BigDecimal outputQty;

    private String outputUnit;

    private Integer runMinutes;

    private BigDecimal unitProductEnergy;

    private Integer deviceCount;

    private Integer activeDeviceCount;

    /** P5 基线接入前为 null */
    private BigDecimal baseline;

    private BigDecimal deviationRate;

    private Integer anomalyDeviceCount;

    private List<ProfilePointVO> trend = new ArrayList<>();

    private List<EnergySliceVO> structure = new ArrayList<>();

    private List<String> unavailable = new ArrayList<>();
}
