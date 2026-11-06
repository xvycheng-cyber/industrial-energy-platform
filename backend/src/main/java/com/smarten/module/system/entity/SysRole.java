package com.smarten.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleCode;

    private String roleName;

    private String description;

    /** 权限码，逗号分隔；* 表示全部 */
    private String permissions;

    private Integer sortNo;

    private Integer status;

    /** 内置角色不可删除 */
    private Integer builtin;
}
