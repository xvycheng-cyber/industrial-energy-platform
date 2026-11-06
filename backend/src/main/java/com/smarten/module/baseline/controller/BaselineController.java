package com.smarten.module.baseline.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.module.baseline.dto.BaselineModelQuery;
import com.smarten.module.baseline.dto.BaselinePredictRequest;
import com.smarten.module.baseline.dto.BaselineRecordQuery;
import com.smarten.module.baseline.dto.BaselineTrainRequest;
import com.smarten.module.baseline.service.BaselineService;
import com.smarten.module.baseline.vo.BaselineErrorAnalysisVO;
import com.smarten.module.baseline.vo.BaselineModelVO;
import com.smarten.module.baseline.vo.BaselineRecordVO;
import com.smarten.module.baseline.vo.BaselineTrainResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "动态能耗基线")
@RestController
@RequestMapping("/baseline")
@RequiredArgsConstructor
public class BaselineController {

    private final BaselineService baselineService;

    @Operation(summary = "训练基线模型", description = "多变量岭回归 + 异常值剔除 + 回退链，训练后按区间生成预测")
    @PostMapping("/models/train")
    @PreAuthorize("hasAuthority('baseline:train')")
    public Result<BaselineTrainResultVO> train(@Validated @RequestBody BaselineTrainRequest request) {
        return Result.ok(baselineService.train(request));
    }

    @Operation(summary = "按模型生成基线预测", description = "缺省使用当前生效模型；保存预测、工况、模型版本与实际偏差")
    @PostMapping("/predict")
    @PreAuthorize("hasAuthority('baseline:train')")
    public Result<Integer> predict(@Validated @RequestBody BaselinePredictRequest request) {
        return Result.ok(baselineService.predict(request));
    }

    @Operation(summary = "模型列表")
    @GetMapping("/models")
    @PreAuthorize("hasAuthority('baseline:view')")
    public Result<PageResult<BaselineModelVO>> models(BaselineModelQuery query) {
        return Result.ok(baselineService.models(query));
    }

    @Operation(summary = "模型详情")
    @GetMapping("/models/{id}")
    @PreAuthorize("hasAuthority('baseline:view')")
    public Result<BaselineModelVO> detail(@PathVariable Long id) {
        return Result.ok(baselineService.modelDetail(id));
    }

    @Operation(summary = "启用/切换版本")
    @PostMapping("/models/{id}/enable")
    @PreAuthorize("hasAuthority('baseline:manage')")
    public Result<Void> enable(@PathVariable Long id) {
        baselineService.enable(id);
        return Result.ok();
    }

    @Operation(summary = "停用模型")
    @PostMapping("/models/{id}/disable")
    @PreAuthorize("hasAuthority('baseline:manage')")
    public Result<Void> disable(@PathVariable Long id) {
        baselineService.disable(id);
        return Result.ok();
    }

    @Operation(summary = "基线明细（基线 vs 实际）")
    @GetMapping("/records")
    @PreAuthorize("hasAuthority('baseline:view')")
    public Result<PageResult<BaselineRecordVO>> records(BaselineRecordQuery query) {
        return Result.ok(baselineService.records(query));
    }

    @Operation(summary = "模型误差分析")
    @GetMapping("/models/{id}/error")
    @PreAuthorize("hasAuthority('baseline:view')")
    public Result<BaselineErrorAnalysisVO> error(@PathVariable Long id) {
        return Result.ok(baselineService.errorAnalysis(id));
    }
}
