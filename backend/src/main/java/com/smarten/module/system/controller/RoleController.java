package com.smarten.module.system.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.dto.StatusDTO;
import com.smarten.module.system.dto.RoleQueryDTO;
import com.smarten.module.system.dto.RoleSaveDTO;
import com.smarten.module.system.service.RoleService;
import com.smarten.module.system.vo.RoleVO;
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

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色分页")
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<PageResult<RoleVO>> page(@Valid RoleQueryDTO query) {
        return Result.ok(roleService.page(query));
    }

    @Operation(summary = "全部启用角色（下拉用）")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<RoleVO>> all() {
        return Result.ok(roleService.listAll());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.ok(roleService.detail(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:create')")
    public Result<Long> create(@Valid @RequestBody RoleSaveDTO dto) {
        return Result.ok(roleService.create(dto));
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveDTO dto) {
        roleService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用角色")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:role:update')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        roleService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
