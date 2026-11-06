package com.smarten.module.datalake.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "清洗明细查询参数")
public class DetailQueryDTO extends PageQuery {

    private Long meterId;

    private Long deviceId;

    private Long enterpriseId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;

    @Schema(description = "质量标记：1正常 2越界 3缺失补全 4重复 5人工修正")
    private Integer qualityFlag;
}
