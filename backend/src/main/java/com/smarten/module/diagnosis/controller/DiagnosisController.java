package com.smarten.module.diagnosis.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.module.diagnosis.dto.AnomalyHandleRequest;
import com.smarten.module.diagnosis.dto.AnomalyQuery;
import com.smarten.module.diagnosis.dto.DiagnosisScanRequest;
import com.smarten.module.diagnosis.dto.ReportConfirmRequest;
import com.smarten.module.diagnosis.dto.RuleUpdateRequest;
import com.smarten.module.diagnosis.service.DiagnosisRuleService;
import com.smarten.module.diagnosis.service.DiagnosisService;
import com.smarten.module.diagnosis.vo.AnomalyVO;
import com.smarten.module.diagnosis.vo.DiagnosisOverviewVO;
import com.smarten.module.diagnosis.vo.DiagnosisReportVO;
import com.smarten.module.diagnosis.vo.DiagnosisRuleVO;
import com.smarten.module.diagnosis.vo.DiagnosisScanResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "异常诊断")
@RestController
@RequestMapping("/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final DiagnosisRuleService ruleService;

    @Operation(summary = "执行诊断扫描", description = "手动触发；同一时刻只允许一个扫描任务")
    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('diagnosis:scan')")
    public Result<DiagnosisScanResultVO> scan(@RequestBody(required = false) DiagnosisScanRequest request) {
        return Result.ok(diagnosisService.scan(request == null ? new DiagnosisScanRequest() : request));
    }

    @Operation(summary = "异常列表")
    @GetMapping("/anomalies")
    @PreAuthorize("hasAuthority('diagnosis:view')")
    public Result<PageResult<AnomalyVO>> anomalies(AnomalyQuery query) {
        return Result.ok(diagnosisService.page(query));
    }

    @Operation(summary = "异常详情")
    @GetMapping("/anomalies/{id}")
    @PreAuthorize("hasAuthority('diagnosis:view')")
    public Result<AnomalyVO> anomaly(@PathVariable Long id) {
        return Result.ok(diagnosisService.detail(id));
    }

    @Operation(summary = "处理异常（确认/误报/转诊断/关闭）")
    @PostMapping("/anomalies/{id}/handle")
    @PreAuthorize("hasAuthority('diagnosis:handle')")
    public Result<Void> handle(@PathVariable Long id, @Validated @RequestBody AnomalyHandleRequest request) {
        diagnosisService.handle(id, request);
        return Result.ok();
    }

    @Operation(summary = "按异常查询诊断报告")
    @GetMapping("/anomalies/{id}/report")
    @PreAuthorize("hasAuthority('diagnosis:view')")
    public Result<DiagnosisReportVO> reportByAnomaly(@PathVariable Long id) {
        return Result.ok(diagnosisService.reportByAnomaly(id));
    }

    @Operation(summary = "诊断报告详情")
    @GetMapping("/reports/{id}")
    @PreAuthorize("hasAuthority('diagnosis:view')")
    public Result<DiagnosisReportVO> report(@PathVariable Long id) {
        return Result.ok(diagnosisService.report(id));
    }

    @Operation(summary = "人工确认故障原因", description = "与系统推断原因区分保存")
    @PostMapping("/reports/{id}/confirm")
    @PreAuthorize("hasAuthority('diagnosis:handle')")
    public Result<Void> confirm(@PathVariable Long id, @Validated @RequestBody ReportConfirmRequest request) {
        diagnosisService.confirmCause(id, request.getConfirmedCause());
        return Result.ok();
    }

    @Operation(summary = "诊断总览（趋势、分类、状态）")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('diagnosis:view')")
    public Result<DiagnosisOverviewVO> overview(
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(diagnosisService.overview(enterpriseId, startDate, endDate));
    }

    @Operation(summary = "规则列表")
    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('diagnosis:view')")
    public Result<List<DiagnosisRuleVO>> rules() {
        return Result.ok(ruleService.list());
    }

    @Operation(summary = "更新规则", description = "仅允许白名单 JSON 字段，版本号自动递增")
    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('diagnosis:rule')")
    public Result<DiagnosisRuleVO> updateRule(@PathVariable Long id, @RequestBody RuleUpdateRequest request) {
        return Result.ok(ruleService.update(id, request));
    }

    @Operation(summary = "启用/停用规则")
    @PostMapping("/rules/{id}/toggle")
    @PreAuthorize("hasAuthority('diagnosis:rule')")
    public Result<Void> toggleRule(@PathVariable Long id, @RequestParam boolean enabled) {
        ruleService.toggle(id, enabled);
        return Result.ok();
    }
}
