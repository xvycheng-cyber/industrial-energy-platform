package com.smarten.module.system.service;

import com.smarten.common.api.PageResult;
import com.smarten.module.system.dto.UserQueryDTO;
import com.smarten.module.system.dto.UserSaveDTO;
import com.smarten.module.system.vo.UserVO;

public interface UserService {

    PageResult<UserVO> page(UserQueryDTO query);

    UserVO detail(Long id);

    Long create(UserSaveDTO dto);

    void update(Long id, UserSaveDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String rawPassword);
}
