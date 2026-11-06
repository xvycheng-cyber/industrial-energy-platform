package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "KPI 卡片")
public class KpiCardVO {

    private String key;

    private String label;

    private BigDecimal value;

    private String unit;

    /** 数据是否可用；false 时前端展示「待接入」 */
    private boolean available = true;

    private String hint;
}
