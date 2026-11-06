package com.smarten.module.rectification.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.module.rectification.dto.FeedbackRequest;
import com.smarten.module.rectification.dto.TaskAssignRequest;
import com.smarten.module.rectification.dto.TaskAuditRequest;
import com.smarten.module.rectification.dto.TaskCreateRequest;
import com.smarten.module.rectification.dto.TaskQuery;
import com.smarten.module.rectification.entity.SysFile;
import com.smarten.module.rectification.service.FileStorageService;
import com.smarten.module.rectification.service.RectificationService;
import com.smarten.module.rectification.vo.AssigneeVO;
import com.smarten.module.rectification.vo.RectificationStatsVO;
import com.smarten.module.rectification.vo.RectificationTaskDetailVO;
import com.smarten.module.rectification.vo.RectificationTaskVO;
import com.smarten.security.DataScopeHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "整改任务")
@RestController
@RequestMapping("/rectification")
@RequiredArgsConstructor
public class RectificationController {

    private final RectificationService rectificationService;
    private final FileStorageService fileStorageService;
    private final DataScopeHelper dataScope;

    @Operation(summary = "任务列表", description = "支持状态、关键字、设备、负责人、超期筛选")
    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('rectify:view')")
    public Result<PageResult<RectificationTaskVO>> tasks(TaskQuery query) {
        return Result.ok(rectificationService.page(query));
    }

    @Operation(summary = "任务详情（含反馈与诊断结论）")
    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('rectify:view')")
    public Result<RectificationTaskDetailVO> detail(@PathVariable Long id) {
        return Result.ok(rectificationService.detail(id));
    }

    @Operation(summary = "创建整改任务", description = "可从诊断报告创建，也可手工指定设备")
    @PostMapping("/tasks")
    @PreAuthorize("hasAuthority('rectify:create')")
    public Result<RectificationTaskDetailVO> create(@Validated @RequestBody TaskCreateRequest request) {
        return Result.ok(rectificationService.create(request));
    }

    @Operation(summary = "分配任务")
    @PostMapping("/tasks/{id}/assign")
    @PreAuthorize("hasAuthority('rectify:assign')")
    public Result<Void> assign(@PathVariable Long id, @Validated @RequestBody TaskAssignRequest request) {
        rectificationService.assign(id, request);
        return Result.ok();
    }

    @Operation(summary = "转派任务")
    @PostMapping("/tasks/{id}/transfer")
    @PreAuthorize("hasAuthority('rectify:assign')")
    public Result<Void> transfer(@PathVariable Long id, @Validated @RequestBody TaskAssignRequest request) {
        rectificationService.transfer(id, request);
        return Result.ok();
    }

    @Operation(summary = "审核", description = "通过后进入效果验证中，不能直接完成")
    @PostMapping("/tasks/{id}/audit")
    @PreAuthorize("hasAuthority('rectify:audit')")
    public Result<Void> audit(@PathVariable Long id, @RequestBody TaskAuditRequest request) {
        rectificationService.audit(id, request);
        return Result.ok();
    }

    @Operation(summary = "效果验证")
    @PostMapping("/tasks/{id}/verify")
    @PreAuthorize("hasAuthority('rectify:verify')")
    public Result<Void> verify(@PathVariable Long id, @RequestBody TaskAuditRequest request) {
        rectificationService.verify(id, request);
        return Result.ok();
    }

    @Operation(summary = "撤销任务")
    @PostMapping("/tasks/{id}/cancel")
    @PreAuthorize("hasAuthority('rectify:assign')")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        rectificationService.cancel(id, reason);
        return Result.ok();
    }

    @Operation(summary = "可分配工程师")
    @GetMapping("/assignees")
    @PreAuthorize("hasAuthority('rectify:assign')")
    public Result<List<AssigneeVO>> assignees() {
        return Result.ok(rectificationService.assignees());
    }

    @Operation(summary = "整改统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('rectify:view')")
    public Result<RectificationStatsVO> stats() {
        return Result.ok(rectificationService.stats());
    }

    @Operation(summary = "上传现场图片", description = "仅 jpg/png/webp，单文件 ≤5MB")
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('rectify:create')")
    public Result<SysFile> upload(@RequestParam("file") @NotNull MultipartFile file) {
        return Result.ok(fileStorageService.saveImage(file, "RECTIFY", dataScope.currentEnterpriseId()));
    }

    @Operation(summary = "Web 端提交反馈（可选）")
    @PostMapping("/tasks/{id}/feedback")
    @PreAuthorize("hasAuthority('rectify:view')")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody FeedbackRequest request) {
        Long userId = com.smarten.common.util.SecurityUtils.getUserIdOrNull();
        rectificationService.feedback(id, userId, request);
        return Result.ok();
    }
}
