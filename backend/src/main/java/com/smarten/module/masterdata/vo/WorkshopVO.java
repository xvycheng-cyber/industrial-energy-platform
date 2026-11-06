package com.smarten.module.masterdata.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "车间")
public class WorkshopVO {

    private Long id;

    private Long enterpriseId;

    private String enterpriseName;

    private String code;

    private String name;

    private String manager;

    private BigDecimal area;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
