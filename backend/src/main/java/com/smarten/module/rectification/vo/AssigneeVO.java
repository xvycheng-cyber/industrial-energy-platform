package com.smarten.module.rectification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "可分配工程师")
public class AssigneeVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;
}
