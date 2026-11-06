package com.smarten.module.rectification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Schema(description = "整改统计")
public class RectificationStatsVO {

    private long total;

    private long overdue;

    private Map<String, Long> byStatus = new LinkedHashMap<>();
}
