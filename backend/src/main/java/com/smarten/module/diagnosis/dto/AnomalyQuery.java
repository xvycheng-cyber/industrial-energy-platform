package com.smarten.module.diagnosis.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "异常查询")
public class AnomalyQuery extends PageQuery {

    private String ruleCode;

    private Integer severity;

    private Integer status;

    private Long deviceId;

    private LocalDate startDate;

    private LocalDate endDate;
}
