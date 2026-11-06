package com.smarten.module.masterdata.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.dto.StatusDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.dto.MeterDTO;
import com.smarten.module.masterdata.service.MeterService;
import com.smarten.module.masterdata.vo.MeterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "计量点")
@RestController
@RequestMapping("/master/meters")
@RequiredArgsConstructor
public class MeterController {

    private final MeterService meterService;

    @Operation(summary = "计量点分页")
    @GetMapping
    @PreAuthorize("hasAuthority('master:meter:list')")
    public Result<PageResult<MeterVO>> page(@Valid MasterQueryDTO query) {
        return Result.ok(meterService.page(query));
    }

    @Operation(summary = "计量点详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:meter:list')")
    public Result<MeterVO> detail(@PathVariable Long id) {
        return Result.ok(meterService.detail(id));
    }

    @Operation(summary = "新增计量点")
    @PostMapping
    @PreAuthorize("hasAuthority('master:meter:create')")
    public Result<Long> create(@Valid @RequestBody MeterDTO dto) {
        return Result.ok(meterService.create(dto));
    }

    @Operation(summary = "修改计量点")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:meter:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MeterDTO dto) {
        meterService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除计量点")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('master:meter:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        meterService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用/停用计量点")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:meter:update')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        meterService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
