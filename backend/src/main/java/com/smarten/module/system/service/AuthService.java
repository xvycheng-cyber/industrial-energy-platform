package com.smarten.module.system.service;

import com.smarten.module.system.dto.LoginDTO;
import com.smarten.module.system.vo.LoginVO;
import com.smarten.module.system.vo.MenuVO;
import com.smarten.module.system.vo.UserProfileVO;

import java.util.List;

public interface AuthService {

    LoginVO login(LoginDTO dto, String clientIp);

    LoginVO refresh(String refreshToken);

    UserProfileVO currentUser();

    List<MenuVO> menus();
}
