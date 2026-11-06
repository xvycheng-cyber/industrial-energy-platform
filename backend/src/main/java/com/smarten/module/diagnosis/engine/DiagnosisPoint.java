package com.smarten.module.diagnosis.engine;

import java.time.LocalDateTime;

/**
 * 单个小时诊断点：实际、基线、偏差与工况。
 */
public record DiagnosisPoint(
        LocalDateTime time,
        double actual,
        double baseline,
        double deviationRate,
        double loadRate,
        double output,
        int runMinutes,
        int status
) {

    public boolean running() {
        return status == 1 && runMinutes > 0;
    }
}
