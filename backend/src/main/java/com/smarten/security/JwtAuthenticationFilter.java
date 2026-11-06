package com.smarten.security;

import com.smarten.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * JWT 认证过滤器。
 *
 * <p>把令牌中的权限码作为 authority 写入 SecurityContext，
 * 角色额外加 {@code ROLE_} 前缀，便于 {@code hasAuthority/hasRole} 两种写法。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 请求属性：记录令牌解析失败原因，供认证入口选择错误码 */
    public static final String ATTR_JWT_ERROR = "smarten.jwt.error";
    public static final String JWT_ERROR_EXPIRED = "expired";
    public static final String JWT_ERROR_INVALID = "invalid";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            authenticate(token, request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtUtil.parse(token);
            if (jwtUtil.isRefreshToken(claims)) {
                // 刷新令牌不能用于访问业务接口
                request.setAttribute(ATTR_JWT_ERROR, JWT_ERROR_INVALID);
                return;
            }
            LoginUser loginUser = new LoginUser(
                    jwtUtil.getUserId(claims),
                    jwtUtil.getUsername(claims),
                    new HashSet<>(jwtUtil.getRoles(claims)),
                    jwtUtil.getEnterpriseId(claims));

            List<GrantedAuthority> authorities = new ArrayList<>();
            for (String permission : jwtUtil.getPermissions(claims)) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
            for (String role : loginUser.roles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException ex) {
            request.setAttribute(ATTR_JWT_ERROR, JWT_ERROR_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT 解析失败: {}", ex.getMessage());
            request.setAttribute(ATTR_JWT_ERROR, JWT_ERROR_INVALID);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
