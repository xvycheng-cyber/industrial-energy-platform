package com.smarten.module.diagnosis.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 结构化规则 JSON 的解析与白名单校验。
 *
 * <p>只允许预定义字段与数值类型；不拼接 SQL、不执行任何脚本。</p>
 */
public final class RuleJson {

    private static final Set<String> ALLOWED = Set.of(
            "threshold", "windowHours", "durationHours", "minLoadRate", "nightStart", "nightEnd",
            "cvThreshold", "peerRate", "forced", "deviceTypes", "description");

    private RuleJson() {
    }

    public static RuleConfig parse(String code, String name, String anomalyType, int priority,
                                   double weight, String conditionJson, String thresholdJson,
                                   Map<String, Object> condition, Map<String, Object> threshold) {
        validate(condition);
        validate(threshold);
        Map<String, Object> merged = new java.util.LinkedHashMap<>(condition);
        if (threshold != null) {
            merged.putAll(threshold);
        }
        return new RuleConfig(
                code,
                name,
                anomalyType,
                priority,
                weight,
                bool(merged, "forced", false),
                integer(merged, "windowHours", 6),
                integer(merged, "durationHours", 1),
                number(merged, "threshold", 30),
                number(merged, "minLoadRate", 20),
                integer(merged, "nightStart", 22),
                integer(merged, "nightEnd", 6),
                number(merged, "cvThreshold", 0.35),
                number(merged, "peerRate", 30),
                deviceTypes(merged)
        );
    }

    /** 校验字段名与取值，未知字段直接拒绝 */
    public static void validate(Map<String, Object> json) {
        if (json == null) {
            return;
        }
        for (String key : json.keySet()) {
            if (!ALLOWED.contains(key)) {
                throw new IllegalArgumentException("规则包含不支持的字段：" + key);
            }
        }
        checkRange(json, "threshold", 0, 10000);
        checkRange(json, "windowHours", 1, 168);
        checkRange(json, "durationHours", 1, 72);
        checkRange(json, "minLoadRate", 0, 100);
        checkRange(json, "nightStart", 0, 23);
        checkRange(json, "nightEnd", 0, 23);
        checkRange(json, "cvThreshold", 0, 10);
        checkRange(json, "peerRate", 0, 1000);
        if (json.containsKey("forced") && !(json.get("forced") instanceof Boolean)) {
            throw new IllegalArgumentException("forced 必须是布尔值");
        }
        if (json.containsKey("deviceTypes")) {
            Object value = json.get("deviceTypes");
            if (!(value instanceof List<?> list) || list.size() > 50) {
                throw new IllegalArgumentException("deviceTypes 必须是不超过 50 项的数组");
            }
            for (Object item : list) {
                if (!(item instanceof String text) || text.isBlank() || text.length() > 32) {
                    throw new IllegalArgumentException("deviceTypes 只能包含非空字符串");
                }
            }
        }
    }

    private static void checkRange(Map<String, Object> json, String key, double min, double max) {
        if (!json.containsKey(key)) {
            return;
        }
        Object value = json.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " 必须是数字");
        }
        double actual = number.doubleValue();
        if (actual < min || actual > max) {
            throw new IllegalArgumentException(key + " 超出范围 [" + min + ", " + max + "]");
        }
    }

    private static double number(Map<String, Object> json, String key, double fallback) {
        Object value = json.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static int integer(Map<String, Object> json, String key, int fallback) {
        return (int) Math.round(number(json, key, fallback));
    }

    private static boolean bool(Map<String, Object> json, String key, boolean fallback) {
        Object value = json.get(key);
        return value instanceof Boolean flag ? flag : fallback;
    }

    private static List<String> deviceTypes(Map<String, Object> json) {
        Object value = json.get("deviceTypes");
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        Set<String> types = new LinkedHashSet<>();
        for (Object item : list) {
            types.add(String.valueOf(item));
        }
        return new ArrayList<>(types);
    }
}
