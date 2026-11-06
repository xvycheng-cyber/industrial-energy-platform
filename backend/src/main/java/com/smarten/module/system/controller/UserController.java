package com.smarten.module.system.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.dto.StatusDTO;
import com.smarten.module.system.dto.PasswordResetDTO;
import com.smarten.module.system.dto.UserQueryDTO;
import com.smarten.module.system.dto.UserSaveDTO;
import com.smarten.module.system.service.UserService;
import com.smarten.module.system.vo.UserVO;
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

@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<UserVO>> page(@Valid UserQueryDTO query) {
        return Result.ok(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    public Result<Long> create(@Valid @RequestBody UserSaveDTO dto) {
        return Result.ok(userService.create(dto));
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserSaveDTO dto) {
        userService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        userService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:reset')")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetDTO dto) {
        userService.resetPassword(id, dto.getPassword());
        return Result.ok();
    }
}
