package com.smarten.common.api;

/**
 * 统一错误码。
 *
 * <p>区间约定：2xx 成功；4xx 协议级错误；5xx 系统错误；
 * 1xxx 系统模块；2xxx 主数据；3xxx 数据湖；4xxx 基线；5xxx 诊断；6xxx 整改；7xxx 核算。</p>
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "数据冲突"),
    PAYLOAD_TOO_LARGE(413, "上传内容过大"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的内容类型"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    INTERNAL_ERROR(500, "系统内部错误"),

    /* 1xxx 系统模块 */
    LOGIN_FAILED(1001, "用户名或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已被禁用"),
    TOKEN_INVALID(1003, "登录凭证无效"),
    TOKEN_EXPIRED(1004, "登录凭证已过期"),
    USERNAME_EXISTS(1005, "用户名已存在"),
    ROLE_NOT_FOUND(1006, "角色不存在"),

    /* 2xxx 主数据 */
    ENTERPRISE_NOT_FOUND(2001, "企业不存在"),
    WORKSHOP_NOT_FOUND(2002, "车间不存在"),
    LINE_NOT_FOUND(2003, "产线不存在"),
    DEVICE_NOT_FOUND(2004, "设备不存在"),
    CODE_DUPLICATED(2005, "编码已存在"),
    DATA_IN_USE(2006, "存在关联数据，无法删除"),

    /* 3xxx 数据湖 */
    INGEST_BATCH_DUPLICATED(3001, "采集批次已存在"),
    METER_NOT_FOUND(3002, "计量仪表不存在"),
    DATA_QUALITY_INVALID(3003, "数据质量不满足要求"),

    /* 4xxx 基线 */
    BASELINE_SAMPLE_NOT_ENOUGH(4001, "样本不足，无法建模"),
    BASELINE_NOT_FOUND(4002, "基线模型不存在"),

    /* 5xxx 诊断 */
    RULE_CONDITION_INVALID(5001, "规则条件不合法"),
    RULE_NOT_FOUND(5002, "诊断规则不存在"),
    ANOMALY_NOT_FOUND(5003, "异常记录不存在"),

    /* 6xxx 整改 */
    TASK_STATUS_ILLEGAL(6001, "非法的任务状态流转"),
    TASK_NOT_FOUND(6002, "整改任务不存在"),

    /* 7xxx 核算 */
    SAVING_DATA_INSUFFICIENT(7001, "观察期数据不足，无法核算");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
