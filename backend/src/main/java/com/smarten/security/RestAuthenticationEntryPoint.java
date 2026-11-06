package com.smarten.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarten.common.api.Result;
import com.smarten.common.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证入口：统一返回 401 JSON，避免 Spring Security 默认重定向到登录页。
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object error = request.getAttribute(JwtAuthenticationFilter.ATTR_JWT_ERROR);
        ResultCode code = JwtAuthenticationFilter.JWT_ERROR_EXPIRED.equals(error)
                ? ResultCode.TOKEN_EXPIRED
                : ResultCode.UNAUTHORIZED;

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Result.fail(code));
    }
}
