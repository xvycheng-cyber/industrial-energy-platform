package com.smarten.module.rectification.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.common.util.SecurityUtils;
import com.smarten.module.diagnosis.dto.AnomalyQuery;
import com.smarten.module.diagnosis.service.DiagnosisService;
import com.smarten.module.diagnosis.vo.AnomalyVO;
import com.smarten.module.rectification.dto.FeedbackRequest;
import com.smarten.module.rectification.entity.SysFile;
import com.smarten.module.rectification.service.FileStorageService;
import com.smarten.module.rectification.service.RectificationService;
import com.smarten.module.rectification.vo.MobileProfileVO;
import com.smarten.module.rectification.vo.RectificationTaskDetailVO;
import com.smarten.module.rectification.vo.RectificationTaskVO;
import com.smarten.module.system.dto.LoginDTO;
import com.smarten.module.system.service.AuthService;
import com.smarten.module.system.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

/**
 * 小程序专用接口：登录、我的任务、反馈、异常、上传、个人中心。
 * 工程师只能操作分配给自己的任务。
 */
@Tag(name = "小程序接口")
@RestController
@RequestMapping("/mobile")
@RequiredArgsConstructor
public class MobileController {

    private final AuthService authService;
    private final RectificationService rectificationService;
    private final DiagnosisService diagnosisService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "小程序登录", description = "校验账号具备工程师/管理员身份后签发 JWT")
    @PostMapping("/auth/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto, HttpServletRequest request) {
        LoginVO login = authService.login(dto, request.getRemoteAddr());
        List<String> roles = login.getUser() == null ? List.of() : login.getUser().getRoles();
        boolean allowed = roles != null && roles.stream()
                .anyMatch(role -> role.equals("ENGINEER") || role.equals("PLATFORM_ADMIN"));
        if (!allowed) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅工程师账号可登录小程序");
        }
        return Result.ok(login);
    }

    @Operation(summary = "我的任务")
    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('rectify:mobile')")
    public Result<PageResult<RectificationTaskVO>> tasks(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(rectificationService.myTasks(requireUserId(), status, pageNum, pageSize));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('rectify:mobile')")
    public Result<RectificationTaskDetailVO> task(@PathVariable Long id) {
        return Result.ok(rectificationService.myTask(id, requireUserId()));
    }

    @Operation(summary = "开始整改")
    @PostMapping("/tasks/{id}/start")
    @PreAuthorize("hasAuthority('rectify:mobile')")
    public Result<Void> start(@PathVariable Long id) {
        rectificationService.start(id, requireUserId());
        return Result.ok();
    }

    @Operation(summary = "提交整改反馈")
    @PostMapping("/tasks/{id}/feedback")
    @PreAuthorize("hasAuthority('rectify:mobile')")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody FeedbackRequest request) {
        rectificationService.feedback(id, requireUserId(), request);
        return Result.ok();
    }

    @Operation(summary = "上传现场图片")
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('rectify:mobile')")
    public Result<SysFile> upload(@RequestParam("file") @NotNull MultipartFile file) {
        Long enterpriseId = SecurityUtils.getLoginUser() == null ? null : SecurityUtils.getLoginUser().enterpriseId();
        return Result.ok(fileStorageService.saveImage(file, "RECTIFY", enterpriseId));
    }

    @Operation(summary = "异常列表（企业授权范围）")
    @GetMapping("/anomalies")
    @PreAuthorize("hasAuthority('rectify:mobile')")
    public Result<PageResult<AnomalyVO>> anomalies(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer severity) {
        AnomalyQuery query = new AnomalyQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setSeverity(severity);
        return Result.ok(diagnosisService.page(query));
    }

    @Operation(summary = "异常详情")
    @GetMapping("/anomalies/{id}")
    @PreAuthorize("hasAuthority('rectify:mobile')")
    public Result<AnomalyVO> anomaly(@PathVariable Long id) {
        return Result.ok(diagnosisService.detail(id));
    }

    @Operation(summary = "个人中心")
    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('rectify:mobile')")
    public Result<MobileProfileVO> profile() {
        return Result.ok(rectificationService.profile(requireUserId()));
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
