package com.smarten.module.saving.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "节能核算查询")
public class SavingQuery extends PageQuery {

    private Long taskId;

    private Long deviceId;

    private Integer status;

    @Schema(description = "结论编码：1达成 2无明显变化 3恶化 4数据不足 5不可比 6停机")
    private Integer conclusionCode;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean includeArchived;
}
