package com.smarten.module.system.service;

import com.smarten.module.system.vo.MenuVO;

import java.util.List;

public interface MenuService {

    /**
     * 根据权限码构建菜单树。
     *
     * @param permissions 权限码集合，包含 * 表示全部
     */
    List<MenuVO> buildMenus(List<String> permissions);
}
