package com.smarten.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private String avatar;

    /** 所属企业；平台管理员为空 */
    private Long enterpriseId;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private String remark;
}
