package com.smarten.module.saving.algorithm;

import java.time.LocalDateTime;

/**
 * 观察期单小时核算点：动态基线、实际能耗、产量与运行状态。
 */
public record SavingPoint(
        LocalDateTime time,
        double baseline,
        double actual,
        double output,
        int runMinutes,
        int status
) {

    public boolean running() {
        return status == 1 && runMinutes > 0;
    }
}
