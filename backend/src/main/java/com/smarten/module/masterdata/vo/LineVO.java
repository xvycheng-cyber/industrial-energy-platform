package com.smarten.module.masterdata.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "产线")
public class LineVO {

    private Long id;

    private Long enterpriseId;

    private String enterpriseName;

    private Long workshopId;

    private String workshopName;

    private String code;

    private String name;

    private String productType;

    private BigDecimal designCapacity;

    private String capacityUnit;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
