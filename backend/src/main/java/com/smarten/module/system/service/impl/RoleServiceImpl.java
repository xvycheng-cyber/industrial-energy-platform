package com.smarten.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.system.dto.RoleQueryDTO;
import com.smarten.module.system.dto.RoleSaveDTO;
import com.smarten.module.system.entity.SysRole;
import com.smarten.module.system.entity.SysUserRole;
import com.smarten.module.system.mapper.SysRoleMapper;
import com.smarten.module.system.mapper.SysUserRoleMapper;
import com.smarten.module.system.service.RoleService;
import com.smarten.module.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public PageResult<RoleVO> page(RoleQueryDTO query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysRole::getRoleCode, keyword).or().like(SysRole::getRoleName, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysRole::getSortNo).orderByAsc(SysRole::getId);

        Page<SysRole> page = roleMapper.selectPage(query.toPage(), wrapper);
        List<RoleVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<RoleVO> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getSortNo))
                .stream().map(this::toVO).toList();
    }

    @Override
    public RoleVO detail(Long id) {
        return toVO(requireRole(id));
    }

    @Override
    public Long create(RoleSaveDTO dto) {
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setPermissions(joinPermissions(dto.getPermissions()));
        role.setSortNo(dto.getSortNo() == null ? 100 : dto.getSortNo());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setBuiltin(0);
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    public void update(Long id, RoleSaveDTO dto) {
        SysRole role = requireRole(id);
        if (role.getBuiltin() != null && role.getBuiltin() == 1
                && !role.getRoleCode().equals(dto.getRoleCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内置角色编码不可修改");
        }
        if (!role.getRoleCode().equals(dto.getRoleCode())) {
            Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, dto.getRoleCode())
                    .ne(SysRole::getId, id));
            if (exists != null && exists > 0) {
                throw new BusinessException(ResultCode.CODE_DUPLICATED, "角色编码已存在");
            }
        }
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setPermissions(joinPermissions(dto.getPermissions()));
        if (dto.getSortNo() != null) {
            role.setSortNo(dto.getSortNo());
        }
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        roleMapper.updateById(role);
    }

    @Override
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (role.getBuiltin() != null && role.getBuiltin() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内置角色不可删除");
        }
        Long inUse = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (inUse != null && inUse > 0) {
            throw new BusinessException(ResultCode.DATA_IN_USE, "角色已分配给用户，无法删除");
        }
        roleMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysRole role = requireRole(id);
        if (role.getBuiltin() != null && role.getBuiltin() == 1 && status == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内置角色不可禁用");
        }
        SysRole update = new SysRole();
        update.setId(id);
        update.setStatus(status);
        roleMapper.updateById(update);
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private String joinPermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        return String.join(",", permissions);
    }

    private RoleVO toVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setPermissions(StringUtils.hasText(role.getPermissions())
                ? Arrays.stream(role.getPermissions().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of());
        vo.setSortNo(role.getSortNo());
        vo.setStatus(role.getStatus());
        vo.setBuiltin(role.getBuiltin());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
