package com.smarten.module.baseline.algorithm;

/**
 * 单次预测结果。
 *
 * @param value          预测能耗
 * @param baselineType   1 回归 / 2 工况分组均值 / 4 历史正常均值 / 5 待机基线
 * @param fallbackReason 回退原因；正常回归时为 null
 */
public record Prediction(double value, int baselineType, String fallbackReason) {

    public static final int REGRESSION = 1;
    public static final int GROUP_MEAN = 2;
    public static final int HISTORICAL = 4;
    public static final int STANDBY = 5;

    public static String typeName(int type) {
        return switch (type) {
            case REGRESSION -> "回归";
            case GROUP_MEAN -> "分组均值";
            case HISTORICAL -> "历史正常均值";
            case STANDBY -> "待机基线";
            default -> "未知";
        };
    }
}
