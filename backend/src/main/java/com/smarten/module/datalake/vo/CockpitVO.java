package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "能源驾驶舱")
public class CockpitVO {

    private LocalDate date;

    /** hour / day / month */
    private String granularity;

    private Integer scopeType;

    private Long scopeId;

    private String scopeName;

    private Long energyTypeId;

    private List<KpiCardVO> kpis = new ArrayList<>();

    private List<ProfilePointVO> trend = new ArrayList<>();

    private List<EnergySliceVO> structure = new ArrayList<>();

    private List<LineRankVO> lineRanking = new ArrayList<>();

    private List<BaselinePointVO> baseline = new ArrayList<>();

    private List<AnomalySliceVO> anomaly = new ArrayList<>();

    private List<SavingPointVO> saving = new ArrayList<>();

    private SankeyGraphVO flow = new SankeyGraphVO();

    private List<String> unavailable = new ArrayList<>();
}
