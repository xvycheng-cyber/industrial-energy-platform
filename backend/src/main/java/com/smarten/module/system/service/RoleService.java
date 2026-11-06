package com.smarten.module.system.service;

import com.smarten.common.api.PageResult;
import com.smarten.module.system.dto.RoleQueryDTO;
import com.smarten.module.system.dto.RoleSaveDTO;
import com.smarten.module.system.vo.RoleVO;

import java.util.List;

public interface RoleService {

    PageResult<RoleVO> page(RoleQueryDTO query);

    List<RoleVO> listAll();

    RoleVO detail(Long id);

    Long create(RoleSaveDTO dto);

    void update(Long id, RoleSaveDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}
