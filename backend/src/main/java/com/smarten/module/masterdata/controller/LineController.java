package com.smarten.module.masterdata.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.dto.StatusDTO;
import com.smarten.module.masterdata.dto.LineDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.service.LineService;
import com.smarten.module.masterdata.vo.LineVO;
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

@Tag(name = "产线")
@RestController
@RequestMapping("/master/lines")
@RequiredArgsConstructor
public class LineController {

    private final LineService lineService;

    @Operation(summary = "产线分页")
    @GetMapping
    @PreAuthorize("hasAuthority('master:line:list')")
    public Result<PageResult<LineVO>> page(@Valid MasterQueryDTO query) {
        return Result.ok(lineService.page(query));
    }

    @Operation(summary = "产线详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:line:list')")
    public Result<LineVO> detail(@PathVariable Long id) {
        return Result.ok(lineService.detail(id));
    }

    @Operation(summary = "新增产线")
    @PostMapping
    @PreAuthorize("hasAuthority('master:line:create')")
    public Result<Long> create(@Valid @RequestBody LineDTO dto) {
        return Result.ok(lineService.create(dto));
    }

    @Operation(summary = "修改产线")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:line:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LineDTO dto) {
        lineService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除产线")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('master:line:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        lineService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用/停用产线")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:line:update')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        lineService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
