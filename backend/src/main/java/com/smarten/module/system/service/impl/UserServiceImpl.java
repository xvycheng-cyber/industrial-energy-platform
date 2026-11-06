package com.smarten.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.common.spi.EnterpriseNameProvider;
import com.smarten.common.util.SecurityUtils;
import com.smarten.module.system.dto.UserQueryDTO;
import com.smarten.module.system.dto.UserSaveDTO;
import com.smarten.module.system.entity.SysRole;
import com.smarten.module.system.entity.SysUser;
import com.smarten.module.system.entity.SysUserRole;
import com.smarten.module.system.mapper.SysRoleMapper;
import com.smarten.module.system.mapper.SysUserMapper;
import com.smarten.module.system.mapper.SysUserRoleMapper;
import com.smarten.module.system.service.UserService;
import com.smarten.module.system.vo.RoleVO;
import com.smarten.module.system.vo.UserVO;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final DataScopeHelper dataScope;
    private final EnterpriseNameProvider enterpriseNameProvider;

    @Override
    public PageResult<UserVO> page(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (dataScope.isPlatformAdmin()) {
            if (query.getEnterpriseId() != null) {
                wrapper.eq(SysUser::getEnterpriseId, query.getEnterpriseId());
            }
        } else {
            wrapper.eq(SysUser::getEnterpriseId, dataScope.requireEnterpriseId());
        }
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> page = userMapper.selectPage(query.toPage(), wrapper);
        List<UserVO> records = toVOList(page.getRecords());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public UserVO detail(Long id) {
        SysUser user = requireUser(id);
        dataScope.checkAccessible(user.getEnterpriseId());
        return toVOList(List.of(user)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserSaveDTO dto) {
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "初始密码不能为空");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        Long enterpriseId = dataScope.resolveEnterpriseId(dto.getEnterpriseId());
        if (enterpriseId != null) {
            assertEnterpriseExists(enterpriseId);
        }
        assertRolesAssignable(dto.getRoleIds());

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setEnterpriseId(enterpriseId);
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setRemark(dto.getRemark());
        userMapper.insert(user);

        replaceRoles(user.getId(), dto.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserSaveDTO dto) {
        SysUser existing = requireUser(id);
        dataScope.checkAccessible(existing.getEnterpriseId());

        if (StringUtils.hasText(dto.getUsername()) && !dto.getUsername().equals(existing.getUsername())) {
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, dto.getUsername())
                    .ne(SysUser::getId, id));
            if (exists != null && exists > 0) {
                throw new BusinessException(ResultCode.USERNAME_EXISTS);
            }
            existing.setUsername(dto.getUsername());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        existing.setRealName(dto.getRealName());
        existing.setPhone(dto.getPhone());
        existing.setEmail(dto.getEmail());
        existing.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getEnterpriseId() != null) {
            Long enterpriseId = dataScope.resolveEnterpriseId(dto.getEnterpriseId());
            assertEnterpriseExists(enterpriseId);
            existing.setEnterpriseId(enterpriseId);
        }
        assertRolesAssignable(dto.getRoleIds());
        userMapper.updateById(existing);

        if (dto.getRoleIds() != null) {
            replaceRoles(id, dto.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = requireUser(id);
        dataScope.checkAccessible(user.getEnterpriseId());
        if (id.equals(SecurityUtils.getUserIdOrNull())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能删除当前登录账号");
        }
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内置管理员不可删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = requireUser(id);
        dataScope.checkAccessible(user.getEnterpriseId());
        if (status == 0 && id.equals(SecurityUtils.getUserIdOrNull())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能禁用当前登录账号");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Override
    public void resetPassword(Long id, String rawPassword) {
        SysUser user = requireUser(id);
        dataScope.checkAccessible(user.getEnterpriseId());
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(update);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void assertEnterpriseExists(Long enterpriseId) {
        if (enterpriseId == null) {
            return;
        }
        Map<Long, String> names = enterpriseNameProvider.findNames(List.of(enterpriseId));
        if (!names.containsKey(enterpriseId)) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
        }
    }

    private void assertRolesAssignable(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != new HashSet<>(roleIds).size()) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        if (!dataScope.isPlatformAdmin()
                && roles.stream().anyMatch(role -> DataScopeHelper.PLATFORM_ADMIN.equals(role.getRoleCode()))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权分配平台管理员角色");
        }
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : new HashSet<>(roleIds)) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private List<UserVO> toVOList(List<SysUser> users) {
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();

        Map<Long, List<Long>> roleIdMap = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .in(SysUserRole::getUserId, userIds))
                .stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));

        Set<Long> allRoleIds = roleIdMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Map<Long, SysRole> roleMap = allRoleIds.isEmpty()
                ? Collections.emptyMap()
                : roleMapper.selectBatchIds(allRoleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));

        Set<Long> enterpriseIds = users.stream().map(SysUser::getEnterpriseId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> enterpriseNames = enterpriseIds.isEmpty()
                ? Collections.emptyMap()
                : enterpriseNameProvider.findNames(enterpriseIds);

        List<UserVO> result = new ArrayList<>(users.size());
        for (SysUser user : users) {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setEnterpriseId(user.getEnterpriseId());
            vo.setEnterpriseName(user.getEnterpriseId() == null ? null : enterpriseNames.get(user.getEnterpriseId()));
            vo.setStatus(user.getStatus());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setCreateTime(user.getCreateTime());
            vo.setRemark(user.getRemark());

            List<RoleVO> roles = new ArrayList<>();
            for (Long roleId : roleIdMap.getOrDefault(user.getId(), List.of())) {
                SysRole role = roleMap.get(roleId);
                if (role == null) {
                    continue;
                }
                RoleVO roleVO = new RoleVO();
                roleVO.setId(role.getId());
                roleVO.setRoleCode(role.getRoleCode());
                roleVO.setRoleName(role.getRoleName());
                roles.add(roleVO);
            }
            vo.setRoles(roles);
            result.add(vo);
        }
        return result;
    }
}
