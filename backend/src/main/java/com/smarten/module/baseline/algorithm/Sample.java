package com.smarten.module.baseline.algorithm;

import java.time.LocalDateTime;

/**
 * 一个训练/预测样本：某设备在某小时的工况与能耗。
 *
 * @param deviceId    设备
 * @param periodStart 统计周期（小时）
 * @param outputQty   产量
 * @param runMinutes  运行时长（分钟）
 * @param loadRate    负荷率 0-100
 * @param ambientTemp 环境温度
 * @param productType 产品类型
 * @param shift       班次 1/2/3
 * @param status      运行状态 1 运行 2 停机 3 检修
 * @param energy      实际能耗（kWh）
 */
public record Sample(
        Long deviceId,
        LocalDateTime periodStart,
        double outputQty,
        double runMinutes,
        double loadRate,
        double ambientTemp,
        String productType,
        int shift,
        int status,
        double energy
) {

    public boolean running() {
        return status == 1 && runMinutes > 0;
    }

    public boolean standby() {
        return !running();
    }

    public boolean valid() {
        return Double.isFinite(outputQty)
                && Double.isFinite(runMinutes)
                && Double.isFinite(loadRate)
                && Double.isFinite(ambientTemp)
                && Double.isFinite(energy)
                && energy >= 0;
    }

    /** 工况分组：负荷档位 + 产品类型 */
    public String conditionGroup() {
        return loadLevel() + "|" + (productType == null || productType.isBlank() ? "UNKNOWN" : productType);
    }

    /** 负荷档位：低 <60，中 60-85，高 >85，停机单独分组 */
    public String loadLevel() {
        if (!running()) {
            return "STOP";
        }
        if (loadRate < 60) {
            return "LOW";
        }
        return loadRate <= 85 ? "MID" : "HIGH";
    }
}
