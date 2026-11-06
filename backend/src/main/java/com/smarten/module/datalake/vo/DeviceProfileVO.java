package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "设备能耗画像")
public class DeviceProfileVO {

    private Long id;

    private String code;

    private String name;

    private String deviceType;

    private String model;

    private BigDecimal ratedPower;

    private String lineName;

    private String workshopName;

    private String enterpriseName;

    private Integer status;

    private LocalDate commissionDate;

    private Integer days;

    private BigDecimal energy;

    private Integer runMinutes;

    private BigDecimal loadRate;

    private BigDecimal outputQty;

    private BigDecimal unitProductEnergy;

    private List<ProfilePointVO> trend = new ArrayList<>();

    private List<EnergySliceVO> structure = new ArrayList<>();

    private List<String> unavailable = new ArrayList<>();
}
