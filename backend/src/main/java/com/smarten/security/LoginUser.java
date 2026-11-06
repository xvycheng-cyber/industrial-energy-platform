package com.smarten.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 登录用户信息（存放于 SecurityContext 的 principal）。
 *
 * @param userId       用户 ID
 * @param username     登录名
 * @param roles        角色编码，如 PLATFORM_ADMIN
 * @param enterpriseId 所属企业；平台管理员为 null
 */
public record LoginUser(Long userId, String username, Set<String> roles, Long enterpriseId) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public LoginUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
