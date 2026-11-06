package com.smarten.common.exception;

import com.smarten.common.api.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。由 {@link GlobalExceptionHandler} 统一转换为 {@code Result}。
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }

    public static BusinessException of(ResultCode resultCode) {
        return new BusinessException(resultCode);
    }
}
