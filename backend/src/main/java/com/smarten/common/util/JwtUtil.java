package com.smarten.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * JWT 生成与解析。
 *
 * <p>算法 HS256，密钥来自配置且长度不少于 32 字节；access token 与 refresh token
 * 通过 {@code type} 声明区分。令牌携带角色与权限码，便于无状态鉴权。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "perms";
    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ENTERPRISE_ID = "enterpriseId";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKey secretKey;
    private final long accessExpireSeconds;
    private final long refreshExpireSeconds;

    public JwtUtil(@Value("${smarten.jwt.secret}") String secret,
                   @Value("${smarten.jwt.access-expire-minutes:120}") long accessExpireMinutes,
                   @Value("${smarten.jwt.refresh-expire-days:7}") long refreshExpireDays) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("smarten.jwt.secret 长度不得少于 " + MIN_SECRET_LENGTH + " 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpireSeconds = accessExpireMinutes * 60;
        this.refreshExpireSeconds = refreshExpireDays * 24 * 60 * 60;
    }

    /** 生成访问令牌 */
    public String createAccessToken(Long userId, String username, Collection<String> roles,
                                    Collection<String> permissions, Long enterpriseId) {
        return buildToken(userId, username, roles, permissions, enterpriseId, TYPE_ACCESS, accessExpireSeconds);
    }

    /** 生成刷新令牌 */
    public String createRefreshToken(Long userId, String username, Collection<String> roles,
                                     Collection<String> permissions, Long enterpriseId) {
        return buildToken(userId, username, roles, permissions, enterpriseId, TYPE_REFRESH, refreshExpireSeconds);
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }

    /** 解析并校验签名与有效期，失败抛出 {@link JwtException} */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    public List<String> getRoles(Claims claims) {
        return stringList(claims.get(CLAIM_ROLES));
    }

    public List<String> getPermissions(Claims claims) {
        return stringList(claims.get(CLAIM_PERMISSIONS));
    }

    public Long getEnterpriseId(Claims claims) {
        Object value = claims.get(CLAIM_ENTERPRISE_ID);
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        return value instanceof List<?> list ? (List<String>) list : List.of();
    }

    private String buildToken(Long userId, String username, Collection<String> roles,
                              Collection<String> permissions, Long enterpriseId,
                              String type, long expireSeconds) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles == null ? List.of() : List.copyOf(roles))
                .claim(CLAIM_PERMISSIONS, permissions == null ? List.of() : List.copyOf(permissions))
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)));
        if (enterpriseId != null) {
            builder.claim(CLAIM_ENTERPRISE_ID, enterpriseId);
        }
        return builder.signWith(secretKey).compact();
    }
}
