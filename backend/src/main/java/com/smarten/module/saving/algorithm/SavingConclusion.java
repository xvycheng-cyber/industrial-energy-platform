package com.smarten.module.saving.algorithm;

/**
 * 核算结论。
 */
public enum SavingConclusion {

    PASSED(1, "节能达成"),
    NO_CHANGE(2, "无明显变化"),
    NEGATIVE(3, "能耗恶化"),
    INSUFFICIENT(4, "数据不足待验证"),
    NOT_COMPARABLE(5, "工况不可比"),
    STOPPED(6, "停机不判定节能");

    private final int code;

    private final String label;

    SavingConclusion(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static String labelOf(Integer code) {
        if (code == null) {
            return "未知";
        }
        for (SavingConclusion value : values()) {
            if (value.code == code) {
                return value.label;
            }
        }
        return "未知";
    }
}
