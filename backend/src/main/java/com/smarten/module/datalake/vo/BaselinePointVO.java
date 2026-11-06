package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "实际与基线对比点")
public class BaselinePointVO {

    private String label;

    private BigDecimal actual;

    private BigDecimal baseline;

    private BigDecimal deviationRate;
}
