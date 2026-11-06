package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "能耗画像")
public class ProfileVO {

    private Integer dimType;

    private Long dimId;

    private String dimName;

    private Integer days;

    private BigDecimal totalEnergy;

    private BigDecimal avgEnergy;

    private BigDecimal maxEnergy;

    private BigDecimal minEnergy;

    private List<ProfilePointVO> series = new ArrayList<>();

    private List<EnergySliceVO> energyTypes = new ArrayList<>();
}
