package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "能耗构成")
public class EnergySliceVO {

    private Long energyTypeId;

    private String energyTypeName;

    private String unit;

    private BigDecimal energyValue;

    private BigDecimal costAmount;
}
