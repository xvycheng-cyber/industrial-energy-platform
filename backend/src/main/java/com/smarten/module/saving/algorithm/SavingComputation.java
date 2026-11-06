package com.smarten.module.saving.algorithm;

import java.util.List;

/**
 * 核算输出（纯计算结果，由 Service 落库）。
 */
public record SavingComputation(
        double baselineTotal,
        double actualTotal,
        double saving,
        double savingRate,
        double savingAmount,
        double completeness,
        int actualDays,
        double outputBefore,
        double outputAfter,
        double outputChangeRate,
        boolean comparable,
        boolean stopped,
        SavingConclusion conclusion,
        String reason,
        List<SavingPoint> points
) {
}
