package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "异常分布切片")
public class AnomalySliceVO {

    private String name;

    private Integer count;

    private BigDecimal ratio;
}
