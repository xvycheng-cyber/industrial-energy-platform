package com.smarten.module.baseline.algorithm;

/**
 * 训练参数。
 *
 * @param minSamples      回归最小有效样本数
 * @param minGroupSamples 工况分组均值的最小样本数
 * @param ridgeLambda     岭正则系数（防止共线导致矩阵奇异）
 * @param outlierSigma    异常值剔除阈值（标准化残差倍数）
 */
public record TrainOptions(
        int minSamples,
        int minGroupSamples,
        double ridgeLambda,
        double outlierSigma
) {

    public static TrainOptions defaults() {
        return new TrainOptions(20, 5, 1.0e-6, 3.0);
    }

    public TrainOptions {
        if (minSamples < 5) {
            minSamples = 5;
        }
        if (minGroupSamples < 1) {
            minGroupSamples = 1;
        }
        if (ridgeLambda <= 0) {
            ridgeLambda = 1.0e-6;
        }
        if (outlierSigma <= 0) {
            outlierSigma = 3.0;
        }
    }
}
