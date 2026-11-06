package com.smarten.module.rectification.enums;

import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 整改任务状态机：所有状态变更必须经过 {@link #check(RectificationStatus, RectificationStatus)}。
 */
public enum RectificationStatus {

    PENDING(0, "待分配"),
    ASSIGNED(1, "待整改"),
    IN_PROGRESS(2, "整改中"),
    PENDING_AUDIT(3, "待审核"),
    VERIFYING(7, "效果验证中"),
    COMPLETED(4, "已完成"),
    REJECTED(5, "已驳回"),
    CANCELLED(6, "已撤销");

    private final int code;

    private final String label;

    RectificationStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    private static final Map<Integer, RectificationStatus> BY_CODE = Map.of(
            0, PENDING, 1, ASSIGNED, 2, IN_PROGRESS, 3, PENDING_AUDIT,
            7, VERIFYING, 4, COMPLETED, 5, REJECTED, 6, CANCELLED
    );

    private static final Map<RectificationStatus, Set<RectificationStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(ASSIGNED, CANCELLED),
            ASSIGNED, EnumSet.of(IN_PROGRESS, ASSIGNED, CANCELLED),
            IN_PROGRESS, EnumSet.of(PENDING_AUDIT, ASSIGNED, CANCELLED),
            PENDING_AUDIT, EnumSet.of(VERIFYING, REJECTED, CANCELLED),
            VERIFYING, EnumSet.of(COMPLETED, IN_PROGRESS, CANCELLED),
            REJECTED, EnumSet.of(ASSIGNED, CANCELLED),
            COMPLETED, EnumSet.noneOf(RectificationStatus.class),
            CANCELLED, EnumSet.noneOf(RectificationStatus.class)
    );

    public static RectificationStatus of(Integer code) {
        RectificationStatus status = code == null ? null : BY_CODE.get(code);
        if (status == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未知的整改状态：" + code);
        }
        return status;
    }

    public boolean canGoTo(RectificationStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static void check(RectificationStatus from, RectificationStatus to) {
        if (!from.canGoTo(to)) {
            throw new BusinessException(ResultCode.TASK_STATUS_ILLEGAL,
                    "不允许的状态流转：" + from.label() + " → " + to.label());
        }
    }

    public static String labelOf(Integer code) {
        RectificationStatus status = code == null ? null : BY_CODE.get(code);
        return status == null ? "未知" : status.label();
    }
}
