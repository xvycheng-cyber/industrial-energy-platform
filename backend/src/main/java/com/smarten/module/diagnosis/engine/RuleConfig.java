package com.smarten.module.diagnosis.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化规则配置：阈值、窗口、持续时间、适用设备类型、优先级与强制标记。
 */
public record RuleConfig(
        String code,
        String name,
        String anomalyType,
        int priority,
        double weight,
        boolean forced,
        int windowHours,
        int durationHours,
        double threshold,
        double minLoadRate,
        int nightStart,
        int nightEnd,
        double cvThreshold,
        double peerRate,
        List<String> deviceTypes
) {

    public RuleConfig {
        if (windowHours <= 0) {
            windowHours = 6;
        }
        if (durationHours <= 0) {
            durationHours = 1;
        }
        deviceTypes = deviceTypes == null ? new ArrayList<>() : new ArrayList<>(deviceTypes);
    }

    public boolean matches(String deviceType) {
        return deviceTypes.isEmpty() || (deviceType != null && deviceTypes.contains(deviceType));
    }
}
