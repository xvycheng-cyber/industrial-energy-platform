package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "企业能耗画像")
public class EnterpriseProfileVO {

    private Long id;

    private String name;

    private String industry;

    private String region;

    private Integer days;

    private BigDecimal totalEnergy;

    private BigDecimal totalCost;

    /** 综合能耗（吨标煤） */
    private BigDecimal coalConsumption;

    private BigDecimal outputTotal;

    private String outputUnit;

    /** 单位产品能耗 */
    private BigDecimal unitProductEnergy;

    /** 单位产值能耗（产值未接入时为 null） */
    private BigDecimal energyIntensity;

    private BigDecimal completenessRate;

    private List<EnergySliceVO> structure = new ArrayList<>();

    private List<ProfilePointVO> trend = new ArrayList<>();

    private RatingVO rating = new RatingVO();

    private List<String> unavailable = new ArrayList<>();
}
