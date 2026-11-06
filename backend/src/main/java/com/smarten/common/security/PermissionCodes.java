package com.smarten.common.security;

import java.util.List;

/**
 * 权限码登记表。
 *
 * <p>角色配置为 {@code *} 时，登录会展开为下表全部权限码，从而支持
 * {@code @PreAuthorize("hasAuthority(...)")} 的精确匹配。
 * 新增接口权限时，必须同步在此登记。</p>
 */
public final class PermissionCodes {

    public static final String ALL_WILDCARD = "*";

    public static final List<String> ALL = List.of(
            // 系统
            "system:user:list", "system:user:create", "system:user:update",
            "system:user:delete", "system:user:reset",
            "system:role:list", "system:role:create", "system:role:update", "system:role:delete",
            // 主数据
            "master:enterprise:list", "master:enterprise:create", "master:enterprise:update", "master:enterprise:delete",
            "master:workshop:list", "master:workshop:create", "master:workshop:update", "master:workshop:delete",
            "master:line:list", "master:line:create", "master:line:update", "master:line:delete",
            "master:device:list", "master:device:create", "master:device:update", "master:device:delete",
            "master:energy:list", "master:energy:create", "master:energy:update", "master:energy:delete",
            "master:meter:list", "master:meter:create", "master:meter:update", "master:meter:delete",
            // 数据湖
            "datalake:raw:list", "datalake:detail:list", "datalake:log:list", "datalake:quality:list",
            "datalake:completeness:list", "datalake:ingest", "datalake:import", "datalake:clean",
            "datalake:simulate", "datalake:ads:view", "datalake:gap:manage",
            // 画像与驾驶舱
            "profile:cockpit:view", "profile:enterprise:view", "profile:line:view",
            "profile:device:view", "profile:hierarchy:view",
            // 三维工厂
            "factory:view",
            // 动态基线
            "baseline:view", "baseline:train", "baseline:manage",
            // 异常诊断
            "diagnosis:view", "diagnosis:scan", "diagnosis:handle", "diagnosis:rule",
            // 整改闭环
            "rectify:view", "rectify:create", "rectify:assign", "rectify:audit", "rectify:verify", "rectify:mobile",
            // 节能核算
            "saving:view", "saving:calc", "saving:audit", "saving:export",
            // 全链路联调
            "demo:init"
    );

    private PermissionCodes() {
    }
}
