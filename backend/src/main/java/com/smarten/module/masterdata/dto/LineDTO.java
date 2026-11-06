package com.smarten.module.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "产线保存参数")
public class LineDTO {

    @Schema(description = "所属企业，非平台管理员可省略")
    private Long enterpriseId;

    @NotNull(message = "所属车间不能为空")
    private Long workshopId;

    @NotBlank(message = "产线编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,64}$", message = "产线编码只能包含字母、数字、下划线、中划线")
    private String code;

    @NotBlank(message = "产线名称不能为空")
    @Size(max = 128)
    private String name;

    @Size(max = 64)
    private String productType;

    @DecimalMin(value = "0", message = "设计产能不能为负数")
    private BigDecimal designCapacity;

    @Size(max = 16)
    private String capacityUnit;

    @Min(0)
    @Max(1)
    private Integer status;

    @Size(max = 255)
    private String remark;
}
