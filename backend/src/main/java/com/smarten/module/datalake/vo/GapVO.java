package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "数据缺口")
public class GapVO {

    private Long id;

    private Long meterId;

    private String meterName;

    private String deviceName;

    private LocalDateTime gapStart;

    private LocalDateTime gapEnd;

    private Integer expectedCount;

    private Integer actualCount;

    private Integer missingCount;

    private Integer status;

    private LocalDateTime fillTime;
}
