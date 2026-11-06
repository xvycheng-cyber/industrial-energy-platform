package com.smarten.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "登录用户档案")
public class UserProfileVO {

    private Long id;

    private String username;

    private String realName;

    private Long enterpriseId;

    private String enterpriseName;

    @Schema(description = "角色编码")
    private List<String> roles;

    @Schema(description = "权限码；* 表示全部")
    private List<String> permissions;
}
