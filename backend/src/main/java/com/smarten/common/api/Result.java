package com.smarten.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应结构。
 *
 * <p>约定：传输层使用 HTTP 状态码（401/403/500 等），业务结果使用 {@code code} 字段；
 * 业务异常与参数校验失败均返回 HTTP 200 + 非 200 的 {@code code}，
 * 由前端拦截器统一提示。</p>
 *
 * @param <T> 业务数据类型
 */
@Getter
@Setter
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务状态码，见 {@link ResultCode} */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 服务端响应时间 */
    private LocalDateTime timestamp;

    /** 链路标识，便于日志排查 */
    private String traceId;

    public Result() {
        this.timestamp = LocalDateTime.now();
        this.traceId = MDC.get("traceId");
    }

    public Result(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
