package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "数据完整率")
public class CompletenessVO {

    private Integer expectedCount;

    private Integer validCount;

    private Integer missingCount;

    private BigDecimal completenessRate;

    private List<DeviceCompletenessVO> devices = new ArrayList<>();
}
