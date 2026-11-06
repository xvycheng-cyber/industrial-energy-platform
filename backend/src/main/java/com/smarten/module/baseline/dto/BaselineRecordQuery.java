package com.smarten.module.baseline.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "基线明细查询")
public class BaselineRecordQuery extends PageQuery {

    private Long baselineId;

    private Integer dimType;

    private Long dimId;
    private Long energyTypeId;

    private LocalDate startDate;

    private LocalDate endDate;

    @Schema(description = "仅看存在偏差的记录")
    private Boolean onlyDeviation;
}
