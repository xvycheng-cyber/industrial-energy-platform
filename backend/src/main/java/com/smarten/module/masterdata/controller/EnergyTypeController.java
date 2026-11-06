package com.smarten.module.masterdata.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.dto.StatusDTO;
import com.smarten.module.masterdata.dto.EnergyTypeDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.service.EnergyTypeService;
import com.smarten.module.masterdata.vo.EnergyTypeVO;
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

@Tag(name = "能源类型")
@RestController
@RequestMapping("/master/energy-types")
@RequiredArgsConstructor
public class EnergyTypeController {

    private final EnergyTypeService energyTypeService;

    @Operation(summary = "能源类型分页")
    @GetMapping
    @PreAuthorize("hasAuthority('master:energy:list')")
    public Result<PageResult<EnergyTypeVO>> page(@Valid MasterQueryDTO query) {
        return Result.ok(energyTypeService.page(query));
    }

    @Operation(summary = "能源类型详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:energy:list')")
    public Result<EnergyTypeVO> detail(@PathVariable Long id) {
        return Result.ok(energyTypeService.detail(id));
    }

    @Operation(summary = "新增能源类型")
    @PostMapping
    @PreAuthorize("hasAuthority('master:energy:create')")
    public Result<Long> create(@Valid @RequestBody EnergyTypeDTO dto) {
        return Result.ok(energyTypeService.create(dto));
    }

    @Operation(summary = "修改能源类型")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:energy:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EnergyTypeDTO dto) {
        energyTypeService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除能源类型")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('master:energy:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        energyTypeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用/停用能源类型")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:energy:update')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        energyTypeService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
