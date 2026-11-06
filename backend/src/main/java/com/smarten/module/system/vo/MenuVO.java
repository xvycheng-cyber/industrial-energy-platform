package com.smarten.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "菜单节点")
public class MenuVO {

    @Schema(description = "唯一标识，前端用作路由 name")
    private String key;

    @Schema(description = "菜单标题")
    private String title;

    @Schema(description = "Element Plus 图标名")
    private String icon;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "子菜单")
    private List<MenuVO> children = new ArrayList<>();

    public MenuVO() {
    }

    public MenuVO(String key, String title, String icon, String path) {
        this.key = key;
        this.title = title;
        this.icon = icon;
        this.path = path;
    }
}
