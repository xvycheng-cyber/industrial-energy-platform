package com.smarten.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.common.security.PermissionCodes;
import com.smarten.common.spi.EnterpriseNameProvider;
import com.smarten.common.util.IpUtil;
import com.smarten.common.util.JwtUtil;
import com.smarten.common.util.SecurityUtils;
import com.smarten.module.system.dto.LoginDTO;
import com.smarten.module.system.entity.SysRole;
import com.smarten.module.system.entity.SysUser;
import com.smarten.module.system.entity.SysUserRole;
import com.smarten.module.system.mapper.SysRoleMapper;
import com.smarten.module.system.mapper.SysUserMapper;
import com.smarten.module.system.mapper.SysUserRoleMapper;
import com.smarten.module.system.service.AuthService;
import com.smarten.module.system.service.MenuService;
import com.smarten.module.system.vo.LoginVO;
import com.smarten.module.system.vo.MenuVO;
import com.smarten.module.system.vo.UserProfileVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ALL_PERMISSION = "*";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MenuService menuService;
    private final EnterpriseNameProvider enterpriseNameProvider;

    @Override
    public LoginVO login(LoginDTO dto, String clientIp) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        List<SysRole> roles = loadRoles(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).toList();
        List<String> permissions = mergePermissions(roles);

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        update.setLastLoginIp(clientIp);
        userMapper.updateById(update);

        log.info("用户登录成功: {} from {}", user.getUsername(), clientIp);
        return buildLoginVO(user, roleCodes, permissions);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        if (!jwtUtil.isRefreshToken(claims)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "请使用刷新令牌");
        }
        SysUser user = userMapper.selectById(jwtUtil.getUserId(claims));
        if (user == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        List<SysRole> roles = loadRoles(user.getId());
        return buildLoginVO(user,
                roles.stream().map(SysRole::getRoleCode).toList(),
                mergePermissions(roles));
    }

    @Override
    public UserProfileVO currentUser() {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        List<SysRole> roles = loadRoles(userId);
        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setRealName(user.getRealName());
        profile.setEnterpriseId(user.getEnterpriseId());
        profile.setEnterpriseName(resolveEnterpriseName(user.getEnterpriseId()));
        profile.setRoles(roles.stream().map(SysRole::getRoleCode).toList());
        profile.setPermissions(mergePermissions(roles));
        return profile;
    }

    @Override
    public List<MenuVO> menus() {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        List<SysRole> roles = loadRoles(userId);
        return menuService.buildMenus(mergePermissions(roles));
    }

    private LoginVO buildLoginVO(SysUser user, List<String> roleCodes, List<String> permissions) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(jwtUtil.createAccessToken(user.getId(), user.getUsername(), roleCodes, permissions, user.getEnterpriseId()));
        vo.setRefreshToken(jwtUtil.createRefreshToken(user.getId(), user.getUsername(), roleCodes, permissions, user.getEnterpriseId()));
        vo.setExpiresIn(jwtUtil.getAccessExpireSeconds());

        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setRealName(user.getRealName());
        profile.setEnterpriseId(user.getEnterpriseId());
        profile.setEnterpriseName(resolveEnterpriseName(user.getEnterpriseId()));
        profile.setRoles(roleCodes);
        profile.setPermissions(permissions);
        vo.setUser(profile);
        return vo;
    }

    private String resolveEnterpriseName(Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        Map<Long, String> names = enterpriseNameProvider.findNames(Set.of(enterpriseId));
        return names.get(enterpriseId);
    }

    private List<SysRole> loadRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSortNo));
    }

    private List<String> mergePermissions(List<SysRole> roles) {
        Set<String> permissions = new LinkedHashSet<>();
        boolean all = false;
        for (SysRole role : roles) {
            if (role.getPermissions() == null || role.getPermissions().isBlank()) {
                continue;
            }
            for (String item : role.getPermissions().split(",")) {
                String code = item.trim();
                if (code.isEmpty()) {
                    continue;
                }
                if (ALL_PERMISSION.equals(code)) {
                    all = true;
                }
                permissions.add(code);
            }
        }
        return all ? PermissionCodes.ALL : new ArrayList<>(permissions);
    }
}
