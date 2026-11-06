package com.smarten.module.demo.controller;

import com.smarten.common.api.Result;
import com.smarten.module.demo.service.DemoDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "全链路联调")
@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoDataService demoDataService;

    @Operation(summary = "生成全链路演示数据", description = "幂等；创建 1 企业/3 车间/6 产线/30 设备与 30 天多能源模拟数据，并触发基线、诊断、整改、核算")
    @PostMapping("/full-chain")
    @PreAuthorize("hasAuthority('demo:init')")
    public Result<Map<String, Object>> fullChain(@RequestParam(defaultValue = "30") int days,
                                                 @RequestParam(required = false) Integer trainDays) {
        int training = trainDays == null ? Math.max(2, days - 7) : trainDays;
        return Result.ok(demoDataService.run(days, training));
    }

    @Operation(summary = "重置联调演示数据", description = "仅清理 DEMO-FULL 演示企业的业务数据，不触碰其他企业")
    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('demo:init')")
    public Result<Map<String, Object>> reset() {
        return Result.ok(demoDataService.reset());
    }

    @Operation(summary = "联调数据状态")
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('demo:init')")
    public Result<Map<String, Object>> status() {
        return Result.ok(demoDataService.status());
    }
}
