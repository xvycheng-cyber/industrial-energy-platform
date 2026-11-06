package com.smarten.module.datalake.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.module.datalake.dto.AggregateQueryDTO;
import com.smarten.module.datalake.dto.CleanRequestDTO;
import com.smarten.module.datalake.dto.DetailQueryDTO;
import com.smarten.module.datalake.dto.LogQueryDTO;
import com.smarten.module.datalake.dto.RawQueryDTO;
import com.smarten.module.datalake.service.DatalakeAggregateService;
import com.smarten.module.datalake.service.DatalakeCleanService;
import com.smarten.module.datalake.service.DatalakeQueryService;
import com.smarten.module.datalake.vo.AggregateVO;
import com.smarten.module.datalake.vo.DetailVO;
import com.smarten.module.datalake.vo.LogVO;
import com.smarten.module.datalake.vo.RawDataVO;
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

@Tag(name = "数据湖-查询与清洗")
@RestController
@RequestMapping("/datalake")
@RequiredArgsConstructor
public class DatalakeQueryController {

    private final DatalakeQueryService queryService;
    private final DatalakeCleanService cleanService;
    private final DatalakeAggregateService aggregateService;

    @Operation(summary = "原始数据分页（ODS）")
    @GetMapping("/raw")
    @PreAuthorize("hasAuthority('datalake:raw:list')")
    public Result<PageResult<RawDataVO>> raw(@Valid RawQueryDTO query) {
        return Result.ok(queryService.rawPage(query));
    }

    @Operation(summary = "清洗明细分页（DWD）")
    @GetMapping("/details")
    @PreAuthorize("hasAuthority('datalake:detail:list')")
    public Result<PageResult<DetailVO>> details(@Valid DetailQueryDTO query) {
        return Result.ok(queryService.detailPage(query));
    }

    @Operation(summary = "聚合数据分页（DWS）")
    @GetMapping("/aggregates")
    @PreAuthorize("hasAuthority('datalake:detail:list')")
    public Result<PageResult<AggregateVO>> aggregates(@Valid AggregateQueryDTO query) {
        return Result.ok(queryService.aggregatePage(query));
    }

    @Operation(summary = "入库日志分页")
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('datalake:log:list')")
    public Result<PageResult<LogVO>> logs(@Valid LogQueryDTO query) {
        return Result.ok(queryService.logPage(query));
    }

    @Operation(summary = "触发清洗", description = "按批次或日期清洗待处理原始数据")
    @PostMapping("/clean")
    @PreAuthorize("hasAuthority('datalake:clean')")
    public Result<Integer> clean(@RequestBody(required = false) CleanRequestDTO dto) {
        CleanRequestDTO request = dto == null ? new CleanRequestDTO() : dto;
        if (request.getBatchNo() != null && !request.getBatchNo().isBlank()) {
            return Result.ok(cleanService.cleanBatch(request.getBatchNo()));
        }
        int limit = request.getLimit() == null ? 5000 : request.getLimit();
        return Result.ok(cleanService.cleanPending(request.getDate(), limit));
    }

    @Operation(summary = "重算聚合", description = "按日期重算 DWS；periods 可选（1小时 2日 3月 4五分钟 5十五分钟）")
    @PostMapping("/aggregate")
    @PreAuthorize("hasAuthority('datalake:clean')")
    public Result<Integer> aggregate(@RequestParam LocalDate date,
                                     @RequestParam(required = false) String periods) {
        if (periods == null || periods.isBlank()) {
            return Result.ok(aggregateService.aggregateDay(date));
        }
        java.util.List<Integer> types = java.util.Arrays.stream(periods.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Integer::valueOf).toList();
        return Result.ok(aggregateService.aggregateRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), types));
    }
}