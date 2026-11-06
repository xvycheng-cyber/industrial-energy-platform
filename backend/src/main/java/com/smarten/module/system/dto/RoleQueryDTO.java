package com.smarten.module.system.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色查询参数")
public class RoleQueryDTO extends PageQuery {

    @Schema(description = "关键字：角色编码 / 名称")
    private String keyword;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;
}
