package com.smarten.module.rectification.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "整改任务查询")
public class TaskQuery extends PageQuery {

    private Integer status;

    private String keyword;

    private Long deviceId;

    private Long assigneeId;

    private Boolean overdue;

    private LocalDate startDate;

    private LocalDate endDate;
}
