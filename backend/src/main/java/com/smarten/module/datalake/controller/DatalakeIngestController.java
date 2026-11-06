package com.smarten.module.datalake.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.page.PageQuery;
import com.smarten.module.datalake.dto.IngestBatchDTO;
import com.smarten.module.datalake.dto.SimulateDTO;
import com.smarten.module.datalake.service.DatalakeImportService;
import com.smarten.module.datalake.service.DatalakeIngestService;
import com.smarten.module.datalake.service.DatalakeQueryService;
import com.smarten.module.datalake.service.DatalakeSimulator;
import com.smarten.module.datalake.vo.FailureVO;
import com.smarten.module.datalake.vo.ImportResultVO;
import com.smarten.module.datalake.vo.IngestResultVO;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Tag(name = "数据湖-接入")
@RestController
@RequestMapping("/datalake")
@RequiredArgsConstructor
public class DatalakeIngestController {

    private final DatalakeIngestService ingestService;
    private final DatalakeImportService importService;
    private final DatalakeSimulator simulator;
    private final DatalakeQueryService queryService;

    @Operation(summary = "批量接入", description = "批次号幂等；重复提交不会重复入库")
    @PostMapping("/ingest")
    @PreAuthorize("hasAuthority('datalake:ingest')")
    public Result<IngestResultVO> ingest(@Valid @RequestBody IngestBatchDTO dto) {
        return Result.ok(ingestService.ingest(dto));
    }

    @Operation(summary = "Excel/CSV 导入", description = "列顺序：仪表编号、采集时间、读数、读数类型(可选)")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('datalake:import')")
    public Result<ImportResultVO> importFile(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importFile(file));
    }

    @Operation(summary = "模拟采集", description = "不传参数则生成最近 1 分钟；用于演示与联调")
    @PostMapping("/simulate")
    @PreAuthorize("hasAuthority('datalake:simulate')")
    public Result<Integer> simulate(@RequestBody(required = false) SimulateDTO dto) {
        LocalDateTime end = dto != null && dto.getEnd() != null
                ? dto.getEnd() : LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime start = dto != null && dto.getStart() != null
                ? dto.getStart() : end.minusMinutes(1);
        int step = dto != null && dto.getStepMinutes() != null ? dto.getStepMinutes() : 1;
        return Result.ok(simulator.simulate(start, end, step));
    }

    @Operation(summary = "失败记录分页")
    @GetMapping("/failures")
    @PreAuthorize("hasAuthority('datalake:quality:list')")
    public Result<PageResult<FailureVO>> failures(@Valid PageQuery query,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) Long meterId) {
        return Result.ok(queryService.failurePage(query, status, meterId));
    }
}
