package com.smarten.module.rectification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    private Long enterpriseId;

    private String bizType;

    private String originalName;

    private String storedPath;

    private String url;

    private String contentType;

    private Long fileSize;

    private Long uploaderId;
}
