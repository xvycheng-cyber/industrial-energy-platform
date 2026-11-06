package com.smarten.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "角色保存参数")
public class RoleSaveDTO {

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64)
    @Schema(description = "角色编码，如 ENERGY_ADMIN")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    private String roleName;

    @Size(max = 255)
    private String description;

    @Schema(description = "权限码列表；* 表示全部")
    private java.util.List<String> permissions;

    private Integer sortNo;

    private Integer status;
}
