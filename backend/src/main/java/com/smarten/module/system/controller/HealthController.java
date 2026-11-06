package com.smarten.module.system.controller;

import com.smarten.common.api.Result;
import com.smarten.module.system.vo.HealthVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * 系统健康检查接口（无需认证）。
 */
@Tag(name = "系统", description = "系统状态与健康检查")
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class HealthController {

    private final Environment environment;

    @Value("${spring.application.name:smarten-backend}")
    private String applicationName;

    @Value("${smarten.version:1.0.0}")
    private String version;

    @Operation(summary = "健康检查", description = "返回应用运行状态、激活环境与运行环境信息")
    @GetMapping("/health")
    public Result<HealthVO> health() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        HealthVO health = new HealthVO(
                "UP",
                applicationName,
                version,
                profiles.isEmpty() ? List.of("default") : profiles,
                System.getProperty("java.version"),
                TimeZone.getDefault().getID(),
                LocalDateTime.now());
        return Result.ok(health);
    }
}
