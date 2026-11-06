package com.smarten.module.datalake.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.page.PageQuery;
import com.smarten.module.datalake.dto.BackfillDTO;
import com.smarten.module.datalake.dto.CompletenessQueryDTO;
import com.smarten.module.datalake.service.DatalakeCompletenessService;
import com.smarten.module.datalake.service.DatalakeGapService;
import com.smarten.module.datalake.service.DatalakeQueryService;
import com.smarten.module.datalake.vo.CompletenessVO;
import com.smarten.module.datalake.vo.GapVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "数据湖-质量")
@RestController
@RequestMapping("/datalake")
@RequiredArgsConstructor
public class DatalakeQualityController {

    private final DatalakeCompletenessService completenessService;
    private final DatalakeGapService gapService;
    private final DatalakeQueryService queryService;

    @Operation(summary = "数据质量概览")
    @GetMapping("/quality/summary")
    @PreAuthorize("hasAuthority('datalake:quality:list')")
    public Result<Map<String, Object>> summary(@RequestParam(required = false) LocalDate date,
                                               @RequestParam(required = false) Long enterpriseId) {
        return Result.ok(queryService.qualitySummary(date, enterpriseId));
    }

    @Operation(summary = "数据完整率", description = "按设备、日期、企业统计应采/有效/缺失/完整率")
    @GetMapping("/completeness")
    @PreAuthorize("hasAuthority('datalake:completeness:list')")
    public Result<CompletenessVO> completeness(@Valid CompletenessQueryDTO query) {
        return Result.ok(completenessService.compute(query));
    }

    @Operation(summary = "缺口分页")
    @GetMapping("/gaps")
    @PreAuthorize("hasAuthority('datalake:quality:list')")
    public Result<PageResult<GapVO>> gaps(@Valid PageQuery query,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(queryService.gapPage(query, status));
    }

    @Operation(summary = "缺失检测", description = "按仪表采集间隔逐点比对，生成缺口记录")
    @PostMapping("/gaps/detect")
    @PreAuthorize("hasAuthority('datalake:gap:manage')")
    public Result<Integer> detect(@RequestParam(required = false) LocalDate date,
                                  @RequestParam(required = false) Long enterpriseId) {
        LocalDate day = date == null ? LocalDate.now() : date;
        return Result.ok(gapService.detect(day, enterpriseId));
    }

    @Operation(summary = "缺口补录", description = "按历史均值估算补录，标记为补录数据，不覆盖原始数据")
    @PostMapping("/gaps/backfill")
    @PreAuthorize("hasAuthority('datalake:gap:manage')")
    public Result<Integer> backfill(@Valid @RequestBody BackfillDTO dto) {
        return Result.ok(gapService.backfill(dto.getGapId()));
    }
}
