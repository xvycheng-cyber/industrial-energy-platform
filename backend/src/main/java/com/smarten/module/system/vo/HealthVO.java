package com.smarten.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统健康检查响应。
 *
 * <p>仅反映应用进程状态，不探测数据库；数据库连通性由部署巡检负责。</p>
 */
@Schema(description = "系统健康检查结果")
public record HealthVO(
        @Schema(description = "应用状态", example = "UP") String status,
        @Schema(description = "应用名称", example = "smarten-backend") String application,
        @Schema(description = "应用版本", example = "1.0.0") String version,
        @Schema(description = "激活的配置环境", example = "[\"dev\"]") List<String> profiles,
        @Schema(description = "Java 版本", example = "17.0.21") String javaVersion,
        @Schema(description = "服务器时区", example = "Asia/Shanghai") String timeZone,
        @Schema(description = "服务器当前时间") LocalDateTime serverTime
) {
}
