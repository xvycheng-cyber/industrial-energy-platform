package com.smarten.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "用户信息")
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private Long enterpriseId;

    private String enterpriseName;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    private List<RoleVO> roles;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private String remark;
}
