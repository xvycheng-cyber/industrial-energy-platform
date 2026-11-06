package com.smarten.module.datalake.dto;

import com.smarten.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "原始数据查询参数")
public class RawQueryDTO extends PageQuery {

    private Long meterId;

    private Long deviceId;

    private Long enterpriseId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;

    @Schema(description = "数据质量：1正常 2可疑 3无效")
    private Integer quality;

    private String source;

    private String batchNo;

    @Schema(description = "清洗状态：0待清洗 1已清洗 2已忽略")
    private Integer cleanStatus;
}
