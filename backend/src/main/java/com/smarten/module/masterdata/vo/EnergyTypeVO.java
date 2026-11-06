package com.smarten.module.masterdata.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "能源类型")
public class EnergyTypeVO {

    private Long id;

    private String code;

    private String name;

    private String unit;

    private BigDecimal price;

    private BigDecimal co2Factor;

    private Integer status;

    private LocalDateTime createTime;
}
