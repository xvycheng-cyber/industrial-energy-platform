package com.smarten.module.masterdata.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.dto.StatusDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.dto.WorkshopDTO;
import com.smarten.module.masterdata.service.WorkshopService;
import com.smarten.module.masterdata.vo.WorkshopVO;
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

@Tag(name = "车间")
@RestController
@RequestMapping("/master/workshops")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopService workshopService;

    @Operation(summary = "车间分页")
    @GetMapping
    @PreAuthorize("hasAuthority('master:workshop:list')")
    public Result<PageResult<WorkshopVO>> page(@Valid MasterQueryDTO query) {
        return Result.ok(workshopService.page(query));
    }

    @Operation(summary = "车间详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:workshop:list')")
    public Result<WorkshopVO> detail(@PathVariable Long id) {
        return Result.ok(workshopService.detail(id));
    }

    @Operation(summary = "新增车间")
    @PostMapping
    @PreAuthorize("hasAuthority('master:workshop:create')")
    public Result<Long> create(@Valid @RequestBody WorkshopDTO dto) {
        return Result.ok(workshopService.create(dto));
    }

    @Operation(summary = "修改车间")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:workshop:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WorkshopDTO dto) {
        workshopService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除车间")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('master:workshop:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        workshopService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用/停用车间")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:workshop:update')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        workshopService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
