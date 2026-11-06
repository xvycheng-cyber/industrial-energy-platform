package com.smarten.module.datalake.controller;

import com.smarten.common.api.Result;
import com.smarten.module.datalake.service.DatalakeAdsService;
import com.smarten.module.datalake.vo.DashboardVO;
import com.smarten.module.datalake.vo.ProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "数据湖-应用")
@RestController
@RequestMapping("/datalake/ads")
@RequiredArgsConstructor
public class DatalakeAdsController {

    private final DatalakeAdsService adsService;

    @Operation(summary = "驾驶舱统计")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('datalake:ads:view')")
    public Result<DashboardVO> dashboard(@RequestParam(required = false) LocalDate date,
                                         @RequestParam(required = false) Long enterpriseId) {
        return Result.ok(adsService.dashboard(date, enterpriseId));
    }

    @Operation(summary = "能耗画像", description = "dimType：1企业 2车间 3产线 4设备")
    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('datalake:ads:view')")
    public Result<ProfileVO> profile(@RequestParam(defaultValue = "4") Integer dimType,
                                     @RequestParam(required = false) Long dimId,
                                     @RequestParam(defaultValue = "7") Integer days) {
        return Result.ok(adsService.profile(dimType, dimId, days));
    }
}
