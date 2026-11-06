package com.smarten.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户保存参数")
public class UserSaveDTO {

    @Size(max = 64, message = "用户名过长")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,64}$", message = "用户名只能包含字母、数字、下划线，长度 3-64")
    @Schema(description = "登录名，创建时必填")
    private String username;

    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    @Schema(description = "密码，创建时必填；更新时留空表示不修改")
    private String password;

    @Size(max = 64, message = "姓名过长")
    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128)
    private String email;

    @Schema(description = "所属企业，平台管理员可指定；其他角色固定为本企业")
    private Long enterpriseId;

    @Min(0)
    @Max(1)
    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    @Schema(description = "角色 ID 列表")
    private List<Long> roleIds;

    @Size(max = 255)
    private String remark;
}
