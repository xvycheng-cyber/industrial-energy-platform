package com.smarten.module.diagnosis.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则引擎：对八类异常规则做确定性检测，纯内存计算，不执行 SQL / 脚本。
 *
 * <p>所有阈值、窗口、持续时间、适用设备类型都来自结构化规则配置。</p>
 */
public class DetectionEngine {

    public static final String R_SURGE = "R001";
    public static final String R_SUSTAINED = "R002";
    public static final String R_NO_LOAD = "R003";
    public static final String R_IDLE = "R004";
    public static final String R_UNIT = "R005";
    public static final String R_NIGHT = "R006";
    public static final String R_FLUCTUATION = "R007";
    public static final String R_PEER = "R008";

    public List<Signal> detectAll(List<DeviceSeries> seriesList, List<RuleConfig> rules) {
        if (seriesList == null || seriesList.isEmpty() || rules == null || rules.isEmpty()) {
            return List.of();
        }
        Map<String, Map<java.time.LocalDateTime, Double>> peerMedians = peerMedians(seriesList);
        List<Signal> signals = new ArrayList<>();
        for (DeviceSeries series : seriesList) {
            if (series.points() == null || series.points().isEmpty()) {
                continue;
            }
            Map<java.time.LocalDateTime, Double> peers = peerMedians.getOrDefault(
                    series.deviceType() == null ? "" : series.deviceType(), Map.of());
            List<RuleConfig> applicable = rules.stream()
                    .filter(rule -> rule.matches(series.deviceType()))
                    .sorted(Comparator.comparingInt(RuleConfig::priority))
                    .toList();
            for (RuleConfig rule : applicable) {
                signals.addAll(evaluate(series, rule, peers));
            }
        }
        signals.sort(Comparator.comparing(Signal::start).thenComparing(Signal::ruleCode));
        return signals;
    }

    private List<Signal> evaluate(DeviceSeries series, RuleConfig rule,
                                  Map<java.time.LocalDateTime, Double> peers) {
        return switch (rule.code()) {
            case R_SURGE -> surge(series, rule);
            case R_SUSTAINED -> sustained(series, rule);
            case R_NO_LOAD -> noLoadHigh(series, rule);
            case R_IDLE -> idle(series, rule);
            case R_UNIT -> unitConsumption(series, rule);
            case R_NIGHT -> night(series, rule);
            case R_FLUCTUATION -> fluctuation(series, rule);
            case R_PEER -> peer(series, rule, peers);
            default -> List.of();
        };
    }

    /** R001 能耗突增：偏差超阈值且较上一小时继续上升 */
    private List<Signal> surge(DeviceSeries series, RuleConfig rule) {
        List<Signal> result = new ArrayList<>();
        List<DiagnosisPoint> points = series.points();
        for (int i = 1; i < points.size(); i++) {
            DiagnosisPoint point = points.get(i);
            DiagnosisPoint previous = points.get(i - 1);
            if (point.deviationRate() >= rule.threshold()
                    && point.actual() >= previous.actual() * 1.3
                    && point.actual() > 0) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("previousActual", round(previous.actual()));
                evidence.put("actual", round(point.actual()));
                evidence.put("deviationRate", round(point.deviationRate()));
                result.add(signal(series, rule, point.time(), point.time().plusHours(1),
                        point.actual(), point.baseline(), point.deviationRate(), evidence));
            }
        }
        return result;
    }

    /** R002 持续高能耗：连续 durationHours 小时偏差超阈值，合并为一个事件 */
    private List<Signal> sustained(DeviceSeries series, RuleConfig rule) {
        List<Signal> result = new ArrayList<>();
        List<DiagnosisPoint> points = series.points();
        int runStart = -1;
        for (int i = 0; i <= points.size(); i++) {
            boolean hit = i < points.size() && points.get(i).deviationRate() >= rule.threshold();
            if (hit && runStart < 0) {
                runStart = i;
            }
            if (!hit && runStart >= 0) {
                int length = i - runStart;
                if (length >= rule.durationHours()) {
                    double actual = points.subList(runStart, i).stream().mapToDouble(DiagnosisPoint::actual).average().orElse(0);
                    double baseline = points.subList(runStart, i).stream().mapToDouble(DiagnosisPoint::baseline).average().orElse(0);
                    double deviation = points.subList(runStart, i).stream().mapToDouble(DiagnosisPoint::deviationRate).max().orElse(0);
                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("hours", length);
                    evidence.put("avgActual", round(actual));
                    evidence.put("avgBaseline", round(baseline));
                    result.add(signal(series, rule, points.get(runStart).time(),
                            points.get(i - 1).time().plusHours(1), actual, baseline, deviation, evidence));
                }
                runStart = -1;
            }
        }
        return result;
    }

    /** R003 空载高能耗：低负荷/无产量但能耗显著高于基线 */
    private List<Signal> noLoadHigh(DeviceSeries series, RuleConfig rule) {
        List<Signal> result = new ArrayList<>();
        for (DiagnosisPoint point : series.points()) {
            if (point.loadRate() <= rule.minLoadRate()
                    && point.output() <= 0.1
                    && point.baseline() > 0
                    && point.deviationRate() >= rule.threshold()) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("loadRate", round(point.loadRate()));
                evidence.put("outputQty", round(point.output()));
                evidence.put("actual", round(point.actual()));
                result.add(signal(series, rule, point.time(), point.time().plusHours(1),
                        point.actual(), point.baseline(), point.deviationRate(), evidence));
            }
        }
        return result;
    }

    /** R004 设备空转：停机/未运行仍有明显能耗 */
    private List<Signal> idle(DeviceSeries series, RuleConfig rule) {
        List<Signal> result = new ArrayList<>();
        for (DiagnosisPoint point : series.points()) {
            if (!point.running() && point.actual() > Math.max(rule.threshold(), point.baseline() * 1.5)) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("status", point.status());
                evidence.put("runMinutes", point.runMinutes());
                evidence.put("actual", round(point.actual()));
                result.add(signal(series, rule, point.time(), point.time().plusHours(1),
                        point.actual(), point.baseline(), point.deviationRate(), evidence));
            }
        }
        return result;
    }

    /** R005 单位产品能耗异常：有产量且单耗偏离基线 */
    private List<Signal> unitConsumption(DeviceSeries series, RuleConfig rule) {
        List<Signal> result = new ArrayList<>();
        for (DiagnosisPoint point : series.points()) {
            if (point.output() > 0.1 && point.deviationRate() >= rule.threshold()) {
                double unitActual = point.actual() / point.output();
                double unitBaseline = point.baseline() / point.output();
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("outputQty", round(point.output()));
                evidence.put("unitActual", round(unitActual));
                evidence.put("unitBaseline", round(unitBaseline));
                result.add(signal(series, rule, point.time(), point.time().plusHours(1),
                        point.actual(), point.baseline(), point.deviationRate(), evidence));
            }
        }
        return result;
    }

    /** R006 夜间异常耗能：夜间时段偏差超阈值 */
    private List<Signal> night(DeviceSeries series, RuleConfig rule) {
        List<Signal> result = new ArrayList<>();
        for (DiagnosisPoint point : series.points()) {
            int hour = point.time().getHour();
            boolean inNight = rule.nightStart() <= rule.nightEnd()
                    ? hour >= rule.nightStart() && hour < rule.nightEnd()
                    : hour >= rule.nightStart() || hour < rule.nightEnd();
            if (inNight && point.deviationRate() >= rule.threshold()) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("hour", hour);
                evidence.put("actual", round(point.actual()));
                evidence.put("baseline", round(point.baseline()));
                result.add(signal(series, rule, point.time(), point.time().plusHours(1),
                        point.actual(), point.baseline(), point.deviationRate(), evidence));
            }
        }
        return result;
    }

    /** R007 能耗波动：窗口内稳健变异系数超阈值，命中窗口内偏离中位数的具体小时 */
    private List<Signal> fluctuation(DeviceSeries series, RuleConfig rule) {
        List<Signal> result = new ArrayList<>();
        List<DiagnosisPoint> points = series.points();
        int window = rule.windowHours();
        if (points.size() < window) {
            return result;
        }
        java.util.Set<java.time.LocalDateTime> flagged = new java.util.TreeSet<>();
        java.util.Map<java.time.LocalDateTime, Double> deviationRates = new HashMap<>();
        for (int end = window; end <= points.size(); end++) {
            List<DiagnosisPoint> slice = points.subList(end - window, end);
            double median = median(slice.stream().map(DiagnosisPoint::actual).toList());
            double mad = median(slice.stream()
                    .map(point -> Math.abs(point.actual() - median))
                    .toList());
            double cv = median <= 0 ? 0 : 1.4826 * mad / median;
            if (cv < rule.cvThreshold()) {
                continue;
            }
            for (DiagnosisPoint point : slice) {
                double diff = median <= 0 ? 0 : Math.abs(point.actual() - median) / median;
                double baselineDiff = point.baseline() <= 0 ? 0 : Math.abs(point.actual() - point.baseline()) / point.baseline();
                if (diff >= rule.cvThreshold() * 0.5 && baselineDiff >= 0.3) {
                    flagged.add(point.time());
                    deviationRates.put(point.time(), median <= 0 ? 0 : (point.actual() - median) / median * 100);
                }
            }
        }
        for (DiagnosisPoint point : points) {
            if (!flagged.contains(point.time())) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("windowHours", window);
            evidence.put("actual", round(point.actual()));
            evidence.put("deviationRate", round(deviationRates.getOrDefault(point.time(), 0.0)));
            result.add(signal(series, rule, point.time(), point.time().plusHours(1),
                    point.actual(), point.baseline(), deviationRates.getOrDefault(point.time(), 0.0), evidence));
        }
        return result;
    }

    /** R008 同类设备能耗异常：与同类型设备中位数比较 */
    private List<Signal> peer(DeviceSeries series, RuleConfig rule,
                              Map<java.time.LocalDateTime, Double> peers) {
        List<Signal> result = new ArrayList<>();
        if (peers.isEmpty()) {
            return result;
        }
        for (DiagnosisPoint point : series.points()) {
            Double median = peers.get(point.time());
            if (median == null || median <= 0) {
                continue;
            }
            double diff = (point.actual() - median) / median;
            if (diff * 100 >= rule.peerRate()) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("peerMedian", round(median));
                evidence.put("actual", round(point.actual()));
                evidence.put("peerDeviationRate", round(diff * 100));
                result.add(signal(series, rule, point.time(), point.time().plusHours(1),
                        point.actual(), median, diff * 100, evidence));
            }
        }
        return result;
    }

    private Signal signal(DeviceSeries series, RuleConfig rule, java.time.LocalDateTime start,
                          java.time.LocalDateTime end, double actual, double baseline,
                          double deviation, Map<String, Object> evidence) {
        return new Signal(rule.code(), rule.anomalyType(), series.deviceId(), start, end,
                actual, baseline, deviation, rule.threshold(), 0, evidence);
    }

    /** 同设备类型、同一小时的能耗中位数 */
    private Map<String, Map<java.time.LocalDateTime, Double>> peerMedians(List<DeviceSeries> seriesList) {
        Map<String, Map<java.time.LocalDateTime, List<Double>>> grouped = new HashMap<>();
        for (DeviceSeries series : seriesList) {
            String type = series.deviceType() == null ? "" : series.deviceType();
            for (DiagnosisPoint point : series.points()) {
                grouped.computeIfAbsent(type, key -> new HashMap<>())
                        .computeIfAbsent(point.time(), key -> new ArrayList<>())
                        .add(point.actual());
            }
        }
        Map<String, Map<java.time.LocalDateTime, Double>> result = new HashMap<>();
        grouped.forEach((type, byHour) -> {
            Map<java.time.LocalDateTime, Double> medians = new HashMap<>();
            byHour.forEach((time, values) -> medians.put(time, median(values)));
            result.put(type, medians);
        });
        return result;
    }

    private double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int size = sorted.size();
        return size % 2 == 1 ? sorted.get(size / 2) : (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
