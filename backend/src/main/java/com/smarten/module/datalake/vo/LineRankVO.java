package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "产线能耗排名")
public class LineRankVO {

    private Long lineId;

    private String lineName;

    private BigDecimal energyValue;
}
