package com.smarten.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "角色信息")
public class RoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    @Schema(description = "权限码列表")
    private List<String> permissions;

    private Integer sortNo;

    private Integer status;

    private Integer builtin;

    private LocalDateTime createTime;
}
