package com.smarten.module.saving.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "节能核算请求")
public class SavingCalculateRequest {

    @Schema(description = "观察天数，默认 7")
    private Integer observationDays;

    @Schema(description = "能源单价（元/kWh），默认 0.65")
    private java.math.BigDecimal basePrice;

    @Schema(description = "验证开始时间，缺省取整改完成时间")
    private java.time.LocalDateTime startTime;
}
