package com.smarten.module.datalake.controller;

import com.smarten.common.api.Result;
import com.smarten.module.datalake.service.FactoryService;
import com.smarten.module.datalake.vo.FactoryDeviceDetailVO;
import com.smarten.module.datalake.vo.FactoryLineDetailVO;
import com.smarten.module.datalake.vo.FactorySceneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "三维工厂")
@RestController
@RequestMapping("/factory")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService factoryService;

    @Operation(summary = "工厂场景", description = "返回厂房/产线/设备布局坐标与风险等级")
    @GetMapping("/scene")
    @PreAuthorize("hasAuthority('factory:view')")
    public Result<FactorySceneVO> scene(@RequestParam(required = false) Long enterpriseId) {
        return Result.ok(factoryService.scene(enterpriseId));
    }

    @Operation(summary = "设备详情")
    @GetMapping("/devices/{id}")
    @PreAuthorize("hasAuthority('factory:view')")
    public Result<FactoryDeviceDetailVO> device(@PathVariable Long id) {
        return Result.ok(factoryService.deviceDetail(id));
    }

    @Operation(summary = "产线详情")
    @GetMapping("/lines/{id}")
    @PreAuthorize("hasAuthority('factory:view')")
    public Result<FactoryLineDetailVO> line(@PathVariable Long id) {
        return Result.ok(factoryService.lineDetail(id));
    }
}
