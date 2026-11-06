package com.smarten.module.diagnosis.engine;

/**
 * 异常评分：偏差、持续时间、历史频率、同类差异归一化后加权，输出 0-100 与风险等级。
 *
 * <p>强制规则（如设备空转、夜间异常）即使综合分不高，也至少判定为高风险。</p>
 */
public class AnomalyScoring {

    public record Result(double score, int severity, String level) {
    }

    public static Result score(Signal signal, RuleConfig rule, int historyCount, double peerDeviation) {
        double threshold = Math.max(signal.threshold(), 1);
        double deviation = Math.abs(signal.deviationRate());
        double devNorm = clamp((deviation - threshold) / (threshold * 2.0));
        double durationNorm = clamp(signal.durationMinutes() / 360.0);
        double historyNorm = clamp(historyCount / 5.0);
        double peerNorm = clamp(Math.abs(peerDeviation) / 50.0);

        double score = 100 * (0.45 * devNorm + 0.25 * durationNorm + 0.15 * historyNorm + 0.15 * peerNorm);
        score = clamp(score);

        int severity = severityOf(score);
        if (rule.forced()) {
            score = Math.max(score, 70);
            severity = Math.max(severity, 3);
        }
        return new Result(round(score), severity, levelOf(severity));
    }

    public static int severityOf(double score) {
        if (score >= 80) {
            return 4;
        }
        if (score >= 60) {
            return 3;
        }
        return score >= 40 ? 2 : 1;
    }

    public static String levelOf(int severity) {
        return switch (severity) {
            case 4 -> "严重";
            case 3 -> "高风险";
            case 2 -> "中风险";
            default -> "低风险";
        };
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
