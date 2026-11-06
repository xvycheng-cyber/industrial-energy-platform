package com.smarten.module.rectification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "小程序个人中心")
public class MobileProfileVO {

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private List<String> roles;

    private long pendingCount;

    private long inProgressCount;

    private long completedCount;
}
