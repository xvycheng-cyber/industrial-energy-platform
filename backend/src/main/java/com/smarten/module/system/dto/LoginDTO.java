package com.smarten.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "登录参数")
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名过长")
    @Schema(description = "登录名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    @Schema(description = "密码", example = "Admin@123")
    private String password;
}
