package com.smarten.module.system.controller;

import com.smarten.common.api.Result;
import com.smarten.common.util.IpUtil;
import com.smarten.module.system.dto.LoginDTO;
import com.smarten.module.system.dto.RefreshTokenDTO;
import com.smarten.module.system.service.AuthService;
import com.smarten.module.system.vo.LoginVO;
import com.smarten.module.system.vo.MenuVO;
import com.smarten.module.system.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "认证", description = "登录、刷新令牌、当前用户、菜单")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.ok(authService.login(dto, IpUtil.clientIp(request)));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.ok(authService.refresh(dto.getRefreshToken()));
    }

    @Operation(summary = "当前用户")
    @GetMapping("/me")
    public Result<UserProfileVO> me() {
        return Result.ok(authService.currentUser());
    }

    @Operation(summary = "当前用户菜单")
    @GetMapping("/menus")
    public Result<List<MenuVO>> menus() {
        return Result.ok(authService.menus());
    }
}
