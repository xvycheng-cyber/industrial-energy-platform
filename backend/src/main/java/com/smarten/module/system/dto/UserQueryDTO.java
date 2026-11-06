package com.smarten.module.system.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询参数")
public class UserQueryDTO extends PageQuery {

    @Schema(description = "关键字：用户名 / 姓名 / 手机号")
    private String keyword;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    @Schema(description = "企业 ID，仅平台管理员有效")
    private Long enterpriseId;
}
