package com.smarten.module.diagnosis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "诊断总览")
public class DiagnosisOverviewVO {

    private long total;

    private long openCount;

    private long handledCount;

    private long falsePositiveCount;

    private long highRiskCount;

    private Map<String, Long> byRule = new LinkedHashMap<>();

    private Map<String, Long> bySeverity = new LinkedHashMap<>();

    private Map<String, Long> byStatus = new LinkedHashMap<>();

    private List<TrendPoint> trend = new ArrayList<>();

    private List<DeviceCount> topDevices = new ArrayList<>();

    @Data
    public static class TrendPoint {
        private String date;
        private long count;
        private long high;

        public TrendPoint(String date, long count, long high) {
            this.date = date;
            this.count = count;
            this.high = high;
        }
    }

    @Data
    public static class DeviceCount {
        private Long deviceId;
        private String deviceName;
        private long count;

        public DeviceCount(Long deviceId, String deviceName, long count) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.count = count;
        }
    }
}
