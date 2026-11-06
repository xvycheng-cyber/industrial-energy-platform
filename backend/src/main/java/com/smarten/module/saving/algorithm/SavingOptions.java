package com.smarten.module.saving.algorithm;

/**
 * 核算参数。
 *
 * @param requiredDays    要求观察天数，默认 7
 * @param minCompleteness 数据完整率下限（%）
 * @param outputTolerance 产量可比性允许变化率（0.3 = ±30%）
 * @param minSavingRate   判定“节能达成”的最小节能率（%）
 * @param basePrice       能源单价（元/kWh）
 * @param peakMultiplier  峰段电价系数
 * @param flatMultiplier  平段电价系数
 * @param valleyMultiplier 谷段电价系数
 */
public record SavingOptions(
        int requiredDays,
        double minCompleteness,
        double outputTolerance,
        double minSavingRate,
        double basePrice,
        double peakMultiplier,
        double flatMultiplier,
        double valleyMultiplier
) {

    public static SavingOptions defaults() {
        return new SavingOptions(7, 80, 0.3, 5, 0.65, 1.2, 1.0, 0.5);
    }

    public SavingOptions {
        if (requiredDays <= 0) {
            requiredDays = 7;
        }
        if (minCompleteness <= 0 || minCompleteness > 100) {
            minCompleteness = 80;
        }
        if (outputTolerance <= 0) {
            outputTolerance = 0.3;
        }
        if (minSavingRate < 0) {
            minSavingRate = 5;
        }
        if (basePrice <= 0) {
            basePrice = 0.65;
        }
    }
}
