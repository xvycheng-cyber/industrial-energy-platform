package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "能耗曲线点")
public class ProfilePointVO {

    private LocalDateTime time;

    private String label;

    private BigDecimal energyValue;
}
