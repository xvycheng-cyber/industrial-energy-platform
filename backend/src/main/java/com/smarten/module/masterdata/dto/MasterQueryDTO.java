package com.smarten.module.masterdata.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "主数据通用查询参数")
public class MasterQueryDTO extends PageQuery {

    @Schema(description = "关键字（编码/名称）")
    private String keyword;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "企业 ID，仅平台管理员有效")
    private Long enterpriseId;

    @Schema(description = "车间 ID")
    private Long workshopId;

    @Schema(description = "产线 ID")
    private Long lineId;

    @Schema(description = "设备 ID")
    private Long deviceId;

    @Schema(description = "设备类型")
    private String deviceType;
}
