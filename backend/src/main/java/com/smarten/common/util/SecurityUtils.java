package com.smarten.common.util;

import com.smarten.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户，未登录返回 null */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser loginUser ? loginUser : null;
    }

    /** 获取当前用户 ID，未登录返回 null（用于审计字段填充） */
    public static Long getUserIdOrNull() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.userId();
    }

    /** 获取当前用户名，未登录返回 null */
    public static String getUsernameOrNull() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.username();
    }
}
