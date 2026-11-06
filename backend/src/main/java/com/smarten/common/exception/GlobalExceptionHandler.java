package com.smarten.common.exception;

import com.smarten.common.api.Result;
import com.smarten.common.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>约定：业务异常与参数校验失败返回 HTTP 200，由 {@code code} 表达业务结果；
 * 鉴权失败（401/403）与未预期异常返回对应 HTTP 状态码。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常: {} {} code={} message={}", request.getMethod(), request.getRequestURI(), ex.getCode(), ex.getMessage());
        // 401/403 需要与传输层语义一致，便于前端与网关识别；其余业务错误保持 HTTP 200 + 业务码
        if (ex.getCode() == ResultCode.FORBIDDEN.getCode()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.fail(ex.getCode(), ex.getMessage()));
        }
        if (ex.getCode() == ResultCode.UNAUTHORIZED.getCode()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(ex.getCode(), ex.getMessage()));
        }
        return ResponseEntity.ok(Result.fail(ex.getCode(), ex.getMessage()));
    }

    /** @RequestBody 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST, message));
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST, message));
    }

    /** 方法参数约束校验失败（@Validated 在类上时触发） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(this::formatViolation)
                .collect(Collectors.joining("; "));
        log.warn("参数约束校验失败: {}", message);
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST, message));
    }

    /** 请求体不可读（JSON 格式错误、类型不匹配） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误，请检查 JSON 结构与字段类型"));
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST, "缺少必填参数: " + ex.getParameterName()));
    }

    /** 参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST, "参数类型错误: " + ex.getName()));
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(ResultCode.METHOD_NOT_ALLOWED, ex.getMessage()));
    }

    /** 静态资源 / 接口不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ResultCode.NOT_FOUND, "接口不存在: " + ex.getResourcePath()));
    }

    /** 上传文件超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.ok(Result.fail(ResultCode.PAYLOAD_TOO_LARGE, "上传文件超出大小限制"));
    }

    /** 方法级权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("权限不足: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.fail(ResultCode.FORBIDDEN));
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.error("未处理异常: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR, "系统繁忙，请稍后重试"));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int index = path.lastIndexOf('.');
        return (index >= 0 ? path.substring(index + 1) : path) + ": " + violation.getMessage();
    }
}
