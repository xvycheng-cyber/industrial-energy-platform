package com.smarten.module.datalake.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "入库日志查询参数")
public class LogQueryDTO extends PageQuery {

    private String batchNo;

    private Long enterpriseId;

    @Schema(description = "状态：0进行中 1成功 2部分成功 3失败")
    private Integer status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
