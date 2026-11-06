package com.smarten.module.datalake.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "聚合查询参数")
public class AggregateQueryDTO extends PageQuery {

    @Schema(description = "维度：1企业 2车间 3产线 4设备")
    private Integer dimType;

    private Long dimId;

    private Long energyTypeId;

    @Schema(description = "周期：1小时 2日 3月 4五分钟 5十五分钟")
    private Integer periodType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;
}
