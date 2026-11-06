package com.smarten.module.saving;

import com.smarten.module.saving.algorithm.SavingCalculator;
import com.smarten.module.saving.algorithm.SavingComputation;
import com.smarten.module.saving.algorithm.SavingConclusion;
import com.smarten.module.saving.algorithm.SavingOptions;
import com.smarten.module.saving.algorithm.SavingPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 节能核算：节能、无明显变化、能耗恶化、工况变化、停机、数据不足与分时电价。
 */
class SavingCalculatorTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 1, 0, 0);

    private List<SavingPoint> points(int days, double baseline, double actual,
                                     double output, boolean running, int runMinutes) {
        List<SavingPoint> list = new ArrayList<>();
        for (int hour = 0; hour < days * 24; hour++) {
            list.add(new SavingPoint(START.plusHours(hour), baseline, actual, output,
                    running ? runMinutes : 0, running ? 1 : 2));
        }
        return list;
    }

    private SavingComputation compute(List<SavingPoint> points, int days, double outputBefore) {
        return new SavingCalculator().compute(points, START, START.plusDays(days), outputBefore,
                SavingOptions.defaults());
    }

    @Test
    @DisplayName("节能达成：节能率与金额按基线核算")
    void savingPassed() {
        SavingComputation result = compute(points(7, 10, 8, 30, true, 60), 7, 30);

        assertThat(result.conclusion()).isEqualTo(SavingConclusion.PASSED);
        assertThat(result.saving()).isCloseTo(7 * 24 * 2.0, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.savingRate()).isCloseTo(20.0, org.assertj.core.data.Offset.offset(0.01));
        assertThat(result.savingAmount()).isGreaterThan(result.saving() * 0.5);
        assertThat(result.completeness()).isEqualTo(100.0);
        assertThat(result.comparable()).isTrue();
        assertThat(result.stopped()).isFalse();
    }

    @Test
    @DisplayName("无明显变化：节能率低于阈值")
    void noChange() {
        SavingComputation result = compute(points(7, 10, 9.8, 30, true, 60), 7, 30);
        assertThat(result.conclusion()).isEqualTo(SavingConclusion.NO_CHANGE);
        assertThat(result.savingRate()).isLessThan(5);
    }

    @Test
    @DisplayName("能耗恶化：节能量为负数")
    void negativeSaving() {
        SavingComputation result = compute(points(7, 10, 12, 30, true, 60), 7, 30);
        assertThat(result.conclusion()).isEqualTo(SavingConclusion.NEGATIVE);
        assertThat(result.saving()).isLessThan(0);
        assertThat(result.savingRate()).isLessThan(0);
    }

    @Test
    @DisplayName("产量大幅变化：标记工况不可比，不判节能达成")
    void outputChanged() {
        SavingComputation result = compute(points(7, 10, 7, 45, true, 60), 7, 30);
        assertThat(result.comparable()).isFalse();
        assertThat(result.conclusion()).isEqualTo(SavingConclusion.NOT_COMPARABLE);
        assertThat(result.outputChangeRate()).isGreaterThan(30);
    }

    @Test
    @DisplayName("设备长期停机：不认定为节能成功")
    void stoppedNotSuccess() {
        SavingComputation result = compute(points(7, 10, 1, 0, false, 0), 7, 30);
        assertThat(result.stopped()).isTrue();
        assertThat(result.conclusion()).isEqualTo(SavingConclusion.STOPPED);
    }

    @Test
    @DisplayName("观察期不足：标记待验证")
    void insufficientData() {
        SavingComputation result = compute(points(3, 10, 8, 30, true, 60), 7, 30);
        assertThat(result.conclusion()).isEqualTo(SavingConclusion.INSUFFICIENT);
        assertThat(result.reason()).contains("不足 7 天");
    }

    @Test
    @DisplayName("分时电价：峰段价格高于谷段")
    void timeOfUsePrice() {
        SavingCalculator calculator = new SavingCalculator();
        SavingOptions options = SavingOptions.defaults();
        assertThat(calculator.priceOf(9, options)).isGreaterThan(calculator.priceOf(2, options));
        assertThat(calculator.priceOf(9, options)).isGreaterThan(calculator.priceOf(20, options));
        assertThat(calculator.priceOf(2, options)).isLessThan(calculator.priceOf(20, options));
    }

    @Test
    @DisplayName("重复计算结果一致，可追溯")
    void repeatable() {
        List<SavingPoint> data = points(7, 10, 8, 30, true, 60);
        SavingComputation first = compute(data, 7, 30);
        SavingComputation second = compute(data, 7, 30);
        assertThat(first.saving()).isEqualTo(second.saving());
        assertThat(first.savingRate()).isEqualTo(second.savingRate());
        assertThat(first.savingAmount()).isEqualTo(second.savingAmount());
    }
}
