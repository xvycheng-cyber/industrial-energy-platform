package com.smarten.module.diagnosis;

import com.smarten.module.diagnosis.engine.AnomalyScoring;
import com.smarten.module.diagnosis.engine.DetectionEngine;
import com.smarten.module.diagnosis.engine.DeviceSeries;
import com.smarten.module.diagnosis.engine.DiagnosisPoint;
import com.smarten.module.diagnosis.engine.RuleConfig;
import com.smarten.module.diagnosis.engine.Signal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 独立标注测试集：8 类异常 + 正常数据，输出 P/R/F1 与混淆矩阵。
 *
 * <p>测试集由固定随机种子生成（与演示库、训练数据完全独立），标签只用于评测，
 * 不参与任何规则参数调整之外的训练过程。</p>
 */
class DiagnosisEngineMetricsTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final int HOURS = 96;
    private static final int INJECTION_DEVICES = 6;
    private static final int COMPRESSOR_DEVICES = 4;

    private record LabeledPoint(int deviceIndex, int hour, boolean anomaly) {
    }

    private record Dataset(List<DeviceSeries> series, boolean[][] labels) {
    }

    private List<RuleConfig> rules() {
        return List.of(
                new RuleConfig("R001", "能耗突增", "R001", 10, 1, false, 1, 1, 40, 20, 22, 6, 0.35, 30, List.of()),
                new RuleConfig("R002", "持续高能耗", "R002", 20, 1, false, 3, 3, 25, 20, 22, 6, 0.35, 30, List.of()),
                new RuleConfig("R003", "空载高能耗", "R003", 30, 1, false, 1, 1, 30, 20, 22, 6, 0.35, 30, List.of()),
                new RuleConfig("R004", "设备空转", "R004", 40, 1, true, 1, 1, 0.8, 20, 22, 6, 0.35, 30, List.of()),
                new RuleConfig("R005", "单位产品能耗异常", "R005", 50, 1, false, 1, 1, 30, 20, 22, 6, 0.35, 30, List.of()),
                new RuleConfig("R006", "夜间异常耗能", "R006", 60, 1, false, 1, 1, 30, 20, 22, 6, 0.35, 30, List.of()),
                new RuleConfig("R007", "设备能耗波动", "R007", 70, 1, false, 6, 1, 35, 20, 22, 6, 0.35, 30, List.of()),
                new RuleConfig("R008", "同类设备能耗异常", "R008", 80, 1, false, 1, 1, 30, 20, 22, 6, 0.35, 30, List.of())
        );
    }

    private double baselineOf(int deviceIndex, int hour) {
        double base = deviceIndex < INJECTION_DEVICES ? 8.0 : 5.0;
        return base * (1 + 0.15 * Math.sin((hour - 6) / 24.0 * 2 * Math.PI));
    }

    private Dataset build() {
        Random random = new Random(42);
        int devices = INJECTION_DEVICES + COMPRESSOR_DEVICES;
        boolean[][] labels = new boolean[devices][HOURS];
        double[][][] values = new double[devices][HOURS][4];

        for (int device = 0; device < devices; device++) {
            String type = device < INJECTION_DEVICES ? "注塑机" : "空压机";
            for (int hour = 0; hour < HOURS; hour++) {
                double baseline = baselineOf(device, hour);
                double actual = baseline * (1 + (random.nextDouble() - 0.5) * 0.06);
                values[device][hour] = new double[]{actual, baseline, 60 + random.nextDouble() * 20, baseline * 3};
            }
            // 夜间正常待机：能耗与基线同步下降，不构成异常
            for (int hour = 22; hour < 24; hour++) {
                values[device][hour][0] = values[device][hour][1] * (1 + (random.nextDouble() - 0.5) * 0.04);
            }
            assert type != null;
        }

        // R001 能耗突增（5 处）
        for (int i = 0; i < 5; i++) {
            int device = random.nextInt(devices);
            int hour = 6 + i * 12;
            values[device][hour][0] = values[device][hour][1] * 2.2;
            labels[device][hour] = true;
        }
        // R002 持续高能耗（4 段 × 4 小时）
        for (int i = 0; i < 4; i++) {
            int device = random.nextInt(devices);
            int start = 10 + i * 18;
            for (int offset = 0; offset < 4; offset++) {
                values[device][start + offset][0] = values[device][start + offset][1] * 1.5;
                labels[device][start + offset] = true;
            }
        }
        // R003 空载高能耗（4 处）
        for (int i = 0; i < 4; i++) {
            int device = random.nextInt(devices);
            int hour = 8 + i * 20;
            values[device][hour][0] = values[device][hour][1] * 1.9;
            values[device][hour][2] = 5;
            values[device][hour][3] = 0;
            labels[device][hour] = true;
        }
        // R004 设备空转（5 处）
        for (int i = 0; i < 5; i++) {
            int device = random.nextInt(devices);
            int hour = 3 + i * 17;
            values[device][hour][0] = 1.5;
            values[device][hour][1] = 0.5;
            values[device][hour][2] = 0;
            values[device][hour][3] = 0;
            labels[device][hour] = true;
        }
        // R005 单位产品能耗异常（4 处）
        for (int i = 0; i < 4; i++) {
            int device = random.nextInt(devices);
            int hour = 9 + i * 21;
            values[device][hour][0] = values[device][hour][1] * 1.8;
            values[device][hour][3] = 2;
            labels[device][hour] = true;
        }
        // R006 夜间异常耗能（5 处）
        for (int i = 0; i < 5; i++) {
            int device = random.nextInt(devices);
            int hour = 23;
            int day = i % 3;
            values[device][day * 24 + hour][0] = values[device][day * 24 + hour][1] * 1.8;
            labels[device][day * 24 + hour] = true;
        }
        // R007 能耗波动（4 个 6 小时窗口）
        for (int i = 0; i < 4; i++) {
            int device = random.nextInt(devices);
            int start = 40 + i * 12;
            for (int offset = 0; offset < 6; offset++) {
                values[device][start + offset][0] = values[device][start + offset][1] * (offset % 2 == 0 ? 0.4 : 1.8);
                labels[device][start + offset] = true;
            }
        }
        // R008 同类设备能耗异常（4 处，单台显著高于同类型中位数）
        for (int i = 0; i < 4; i++) {
            int device = i < 2 ? 0 : INJECTION_DEVICES;
            int hour = 14 + i * 9;
            values[device][hour][0] = values[device][hour][1] * 2.4;
            labels[device][hour] = true;
        }

        // 边界样本：刚超阈值应检出；略低于阈值允许漏检（模拟真实噪声下的漏报）
        for (int i = 0; i < 2; i++) {
            int device = 7 + i;
            int hour = 5 + i * 80;
            values[device][hour][0] = values[device][hour][1] * 1.45;
            labels[device][hour] = true;
        }
        for (int i = 0; i < 2; i++) {
            int device = 5 + i;
            int hour = 20 + i * 60;
            values[device][hour][0] = values[device][hour][1] * 1.18;
            labels[device][hour] = true;
        }

        List<DeviceSeries> series = new ArrayList<>();
        for (int device = 0; device < devices; device++) {
            List<DiagnosisPoint> points = new ArrayList<>();
            String type = device < INJECTION_DEVICES ? "注塑机" : "空压机";
            for (int hour = 0; hour < HOURS; hour++) {
                double actual = values[device][hour][0];
                double baseline = values[device][hour][1];
                double load = values[device][hour][2];
                double output = values[device][hour][3];
                int status = output == 0 && load <= 5 ? 2 : 1;
                int runMinutes = status == 2 ? 0 : 60;
                points.add(new DiagnosisPoint(BASE.plusHours(hour), actual, baseline,
                        (actual - baseline) / baseline * 100, load, output, runMinutes, status));
            }
            series.add(new DeviceSeries((long) device + 1, type, 1L, points));
        }
        return new Dataset(series, labels);
    }

    @Test
    @DisplayName("八类异常独立测试集：Recall≥90%，输出混淆矩阵与 P/R/F1")
    void metricsOnLabeledDataset() {
        Dataset dataset = build();
        List<Signal> signals = new DetectionEngine().detectAll(dataset.series(), rules());

        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;
        for (int device = 0; device < dataset.labels().length; device++) {
            final long deviceId = device + 1L;
            for (int hour = 0; hour < HOURS; hour++) {
                LocalDateTime time = BASE.plusHours(hour);
                boolean predicted = signals.stream()
                        .anyMatch(signal -> signal.deviceId() == deviceId
                                && !time.isBefore(signal.start()) && time.isBefore(signal.end()));
                boolean actual = dataset.labels()[device][hour];
                if (predicted && actual) {
                    tp++;
                } else if (predicted) {
                    fp++;
                } else if (actual) {
                    fn++;
                } else {
                    tn++;
                }
            }
        }

        double precision = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        double recall = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);

        System.out.printf("""
                ===== 异常识别独立测试集 =====
                样本点: %d, 规则命中信号: %d
                混淆矩阵: TP=%d FP=%d TN=%d FN=%d
                Precision=%.4f Recall=%.4f F1=%.4f
                =============================
                """, dataset.labels().length * HOURS, signals.size(), tp, fp, tn, fn, precision, recall, f1);

        assertThat(recall).isGreaterThanOrEqualTo(0.90);
        assertThat(precision).isGreaterThanOrEqualTo(0.80);
        assertThat(f1).isGreaterThanOrEqualTo(0.85);
        assertThat(tp + fp + tn + fn).isEqualTo(dataset.labels().length * HOURS);
        assertThat(tp).isGreaterThan(0);

        // 同一小时内可能命中多条规则，但不应产生大规模误报
        assertThat(fp).isLessThan(tp);
    }

    @Test
    @DisplayName("强制规则：低评分仍判定为高风险")
    void forcedRuleKeepsHighRisk() {
        RuleConfig idleRule = new RuleConfig("R004", "设备空转", "R004", 40, 1, true,
                1, 1, 0.8, 20, 22, 6, 0.35, 30, List.of());
        Signal signal = new Signal("R004", "R004", 1L, BASE, BASE.plusHours(1),
                1.0, 0, 0, 0.8, 0, java.util.Map.of());

        AnomalyScoring.Result result = AnomalyScoring.score(signal, idleRule, 0, 0);

        assertThat(result.severity()).isGreaterThanOrEqualTo(3);
        assertThat(result.score()).isGreaterThanOrEqualTo(70);
        assertThat(result.level()).isIn("高风险", "严重");
    }

    @Test
    @DisplayName("同一设备不同工况得到不同风险分")
    void scoreVariesWithDeviationAndDuration() {
        RuleConfig rule = rules().get(1);
        Signal small = new Signal("R002", "R002", 1L, BASE, BASE.plusHours(1),
                10, 8, 25, 25, 0, java.util.Map.of());
        Signal large = new Signal("R002", "R002", 1L, BASE, BASE.plusHours(5),
                20, 8, 150, 25, 40, java.util.Map.of());

        double smallScore = AnomalyScoring.score(small, rule, 0, 0).score();
        double largeScore = AnomalyScoring.score(large, rule, 3, 40).score();

        assertThat(largeScore).isGreaterThan(smallScore);
        assertThat(largeScore).isLessThanOrEqualTo(100);
    }
}
