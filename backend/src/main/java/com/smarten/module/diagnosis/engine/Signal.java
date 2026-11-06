package com.smarten.module.diagnosis.engine;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 规则命中的原始信号，尚未评分与合并。
 */
public record Signal(
        String ruleCode,
        String anomalyType,
        Long deviceId,
        LocalDateTime start,
        LocalDateTime end,
        double actual,
        double baseline,
        double deviationRate,
        double threshold,
        double peerDeviation,
        Map<String, Object> evidence
) {

    public Signal {
        evidence = evidence == null ? new LinkedHashMap<>() : new LinkedHashMap<>(evidence);
    }

    public int durationMinutes() {
        return (int) java.time.Duration.between(start, end).toMinutes();
    }
}
