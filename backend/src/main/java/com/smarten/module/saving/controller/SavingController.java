package com.smarten.module.saving.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.module.saving.dto.SavingCalculateRequest;
import com.smarten.module.saving.dto.SavingQuery;
import com.smarten.module.saving.service.SavingService;
import com.smarten.module.saving.vo.SavingResultVO;
import com.smarten.module.saving.vo.SavingSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Tag(name = "节能效果核算")
@RestController
@RequestMapping("/saving")
@RequiredArgsConstructor
public class SavingController {

    private final SavingService savingService;

    @Operation(summary = "执行核算", description = "按观察期逐小时修正基线核算，可重复执行并保留历史版本")
    @PostMapping("/tasks/{taskId}/calculate")
    @PreAuthorize("hasAuthority('saving:calc')")
    public Result<SavingResultVO> calculate(@PathVariable Long taskId,
                                           @RequestBody(required = false) SavingCalculateRequest request) {
        return Result.ok(savingService.calculate(taskId, request == null ? new SavingCalculateRequest() : request));
    }

    @Operation(summary = "核算结果列表")
    @GetMapping("/results")
    @PreAuthorize("hasAuthority('saving:view')")
    public Result<PageResult<SavingResultVO>> results(SavingQuery query) {
        return Result.ok(savingService.page(query));
    }

    @Operation(summary = "核算详情（含逐小时对比与历史版本）")
    @GetMapping("/results/{id}")
    @PreAuthorize("hasAuthority('saving:view')")
    public Result<SavingResultVO> detail(@PathVariable Long id) {
        return Result.ok(savingService.detail(id));
    }

    @Operation(summary = "审核核算结果", description = "通过则完成任务，驳回则退回整改")
    @PostMapping("/results/{id}/confirm")
    @PreAuthorize("hasAuthority('saving:audit')")
    public Result<Void> confirm(@PathVariable Long id,
                                @RequestParam boolean pass,
                                @RequestParam(required = false) String remark) {
        savingService.confirm(id, pass, remark);
        return Result.ok();
    }

    @Operation(summary = "节能排名与趋势")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('saving:view')")
    public Result<SavingSummaryVO> summary() {
        return Result.ok(savingService.summary());
    }

    @Operation(summary = "导出核算结果 CSV")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('saving:export')")
    public ResponseEntity<byte[]> export(SavingQuery query) {
        byte[] content = savingService.exportCsv(query);
        String fileName = "saving-results-" + java.time.LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content);
    }
}
