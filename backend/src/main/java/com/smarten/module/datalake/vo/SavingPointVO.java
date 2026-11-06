package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "节能效果趋势点")
public class SavingPointVO {

    private String month;

    private BigDecimal savingEnergy;

    private BigDecimal savingAmount;
}
