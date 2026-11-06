package com.smarten.module.baseline.algorithm;

import java.util.List;

/**
 * 训练结果。
 *
 * @param model           模型（含参数、指标、回退记录）
 * @param normalSamples   参与训练的运行样本数（清洗后、剔除异常值前）
 * @param standbySamples  待机/停机样本数
 * @param rejected        无效样本数（字段缺失、负值等）
 * @param outliers        被异常值规则剔除的样本数
 * @param requiredSamples 回归所需最小样本数
 */
public record TrainResult(
        BaselineModel model,
        int normalSamples,
        int standbySamples,
        int rejected,
        int outliers,
        int requiredSamples
) {
}
