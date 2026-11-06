package com.smarten.module.baseline.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "基线模型查询")
public class BaselineModelQuery extends PageQuery {

    private Integer dimType;

    private Long dimId;

    private Long energyTypeId;

    @Schema(description = "状态：0草稿 1生效 2归档")
    private Integer status;
}
