package com.smarten.module.masterdata.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "企业")
public class EnterpriseVO {

    private Long id;

    private String code;

    private String name;

    private String industry;

    private String region;

    private String address;

    private String contactName;

    private String contactPhone;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
