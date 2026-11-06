package com.smarten.module.diagnosis.engine;

import java.util.List;

/**
 * 单台设备的诊断序列（按时间升序）。
 */
public record DeviceSeries(Long deviceId, String deviceType, Long lineId, List<DiagnosisPoint> points) {
}
