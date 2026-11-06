package com.smarten.module.saving.algorithm;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 节能核算引擎：按观察期逐小时“动态基线 − 实际”，叠加分时电价。
 *
 * <p>不做整改前后总能耗简单相减；基线来自 P5 模型，已包含产量/负荷/温度等工况修正。</p>
 */
public class SavingCalculator {

    public SavingComputation compute(List<SavingPoint> points, LocalDateTime start, LocalDateTime end,
                                     double outputBefore, SavingOptions options) {
        long expectedHours = Math.max(1, Duration.between(start, end).toHours());
        int expectedDays = (int) Math.max(1, Math.ceil(expectedHours / 24.0));

        double baselineTotal = 0;
        double actualTotal = 0;
        double amount = 0;
        Set<java.time.LocalDate> days = new HashSet<>();
        int stoppedHours = 0;
        double outputSum = 0;
        int outputCount = 0;

        for (SavingPoint point : points) {
            baselineTotal += point.baseline();
            actualTotal += point.actual();
            double hourSaving = point.baseline() - point.actual();
            amount += hourSaving * priceOf(point.time().getHour(), options);
            days.add(point.time().toLocalDate());
            if (!point.running()) {
                stoppedHours++;
            }
            if (point.output() > 0) {
                outputSum += point.output();
                outputCount++;
            }
        }

        double completeness = Math.min(100.0, points.size() * 100.0 / expectedHours);
        double outputAfter = outputCount == 0 ? 0 : outputSum / outputCount;
        double outputChange = outputBefore <= 0 ? 0 : (outputAfter - outputBefore) / outputBefore;
        boolean comparable = Math.abs(outputChange) <= options.outputTolerance();
        boolean stopped = stoppedHours * 2 >= Math.max(points.size(), 1);

        double saving = baselineTotal - actualTotal;
        double rate = baselineTotal <= 0 ? 0 : saving / baselineTotal * 100;

        SavingConclusion conclusion;
        String reason;
        if (points.isEmpty() || completeness < options.minCompleteness() || days.size() < options.requiredDays()) {
            conclusion = SavingConclusion.INSUFFICIENT;
            reason = String.format("观察期有效数据 %d 小时（完整率 %.1f%%），不足 %d 天，待继续观察",
                    points.size(), completeness, options.requiredDays());
        } else if (stopped) {
            conclusion = SavingConclusion.STOPPED;
            reason = "观察期内超过一半时间设备停机，不能将低能耗认定为节能成果";
        } else if (!comparable) {
            conclusion = SavingConclusion.NOT_COMPARABLE;
            reason = String.format("观察期平均产量较整改前变化 %.1f%%，超出 ±%.0f%% 的可比范围",
                    outputChange * 100, options.outputTolerance() * 100);
        } else if (saving < 0) {
            conclusion = SavingConclusion.NEGATIVE;
            reason = String.format("实际能耗高于动态基线 %.2f kWh，节能率为负", -saving);
        } else if (rate < options.minSavingRate()) {
            conclusion = SavingConclusion.NO_CHANGE;
            reason = String.format("节能率 %.2f%% 低于判定阈值 %.0f%%，视为无明显变化",
                    rate, options.minSavingRate());
        } else {
            conclusion = SavingConclusion.PASSED;
            reason = String.format("观察期 %d 天，节能率 %.2f%%，达到判定阈值", days.size(), rate);
        }

        return new SavingComputation(
                round(baselineTotal), round(actualTotal), round(saving), round(rate), round(amount),
                round(completeness), days.size(), round(outputBefore), round(outputAfter),
                round(outputChange * 100), comparable, stopped, conclusion, reason, points);
    }

    /** 分时电价：8-11、13-17 峰段；22-6 谷段；其余平段 */
    public double priceOf(int hour, SavingOptions options) {
        if ((hour >= 8 && hour < 11) || (hour >= 13 && hour < 17)) {
            return options.basePrice() * options.peakMultiplier();
        }
        if (hour >= 22 || hour < 6) {
            return options.basePrice() * options.valleyMultiplier();
        }
        return options.basePrice() * options.flatMultiplier();
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
