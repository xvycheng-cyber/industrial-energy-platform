package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "驾驶舱统计")
public class DashboardVO {

    private LocalDate date;

    private BigDecimal totalEnergy;

    private BigDecimal totalCost;

    private BigDecimal completenessRate;

    private Integer deviceCount;

    private Integer todayBatchCount;

    private String lastBatchNo;

    private List<EnergySliceVO> energyTypes = new ArrayList<>();

    private List<DeviceRankVO> topDevices = new ArrayList<>();

    private List<ProfilePointVO> hourly = new ArrayList<>();
}
