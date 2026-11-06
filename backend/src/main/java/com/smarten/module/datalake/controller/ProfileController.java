package com.smarten.module.datalake.controller;

import com.smarten.common.api.Result;
import com.smarten.module.datalake.service.ProfileService;
import com.smarten.module.datalake.vo.CockpitVO;
import com.smarten.module.datalake.vo.DeviceProfileVO;
import com.smarten.module.datalake.vo.EnterpriseProfileVO;
import com.smarten.module.datalake.vo.HierarchyNodeVO;
import com.smarten.module.datalake.vo.LineProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "能耗画像与驾驶舱")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "能源驾驶舱", description = "scopeType：1企业 2车间 3产线 4设备；granularity：hour/day")
    @GetMapping("/cockpit")
    @PreAuthorize("hasAuthority('profile:cockpit:view')")
    public Result<CockpitVO> cockpit(@RequestParam(required = false) LocalDate date,
                                     @RequestParam(required = false) Integer scopeType,
                                     @RequestParam(required = false) Long scopeId,
                                     @RequestParam(required = false) Long energyTypeId,
                                     @RequestParam(required = false) String granularity) {
        return Result.ok(profileService.cockpit(date, scopeType, scopeId, energyTypeId, granularity));
    }

    @Operation(summary = "企业能耗画像")
    @GetMapping("/enterprise/{id}")
    @PreAuthorize("hasAuthority('profile:enterprise:view')")
    public Result<EnterpriseProfileVO> enterprise(@PathVariable Long id,
                                                  @RequestParam(required = false) Integer days,
                                                  @RequestParam(required = false) Long energyTypeId) {
        return Result.ok(profileService.enterpriseProfile(id, days, energyTypeId));
    }

    @Operation(summary = "产线能耗画像")
    @GetMapping("/line/{id}")
    @PreAuthorize("hasAuthority('profile:line:view')")
    public Result<LineProfileVO> line(@PathVariable Long id,
                                      @RequestParam(required = false) Integer days) {
        return Result.ok(profileService.lineProfile(id, days));
    }

    @Operation(summary = "设备能耗画像")
    @GetMapping("/device/{id}")
    @PreAuthorize("hasAuthority('profile:device:view')")
    public Result<DeviceProfileVO> device(@PathVariable Long id,
                                          @RequestParam(required = false) Integer days) {
        return Result.ok(profileService.deviceProfile(id, days));
    }

    @Operation(summary = "层级树", description = "企业→车间→产线→设备，用于下钻")
    @GetMapping("/hierarchy")
    @PreAuthorize("hasAuthority('profile:hierarchy:view')")
    public Result<List<HierarchyNodeVO>> hierarchy(@RequestParam(required = false) Long enterpriseId) {
        return Result.ok(profileService.hierarchy(enterpriseId));
    }
}
