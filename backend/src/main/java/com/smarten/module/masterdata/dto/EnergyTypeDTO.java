package com.smarten.module.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "能源类型保存参数")
public class EnergyTypeDTO {

    @NotBlank(message = "编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{2,32}$", message = "编码只能包含字母、数字、下划线")
    private String code;

    @NotBlank(message = "名称不能为空")
    @Size(max = 64)
    private String name;

    @NotBlank(message = "单位不能为空")
    @Size(max = 16)
    private String unit;

    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "碳排放因子不能为负数")
    private BigDecimal co2Factor;

    @Min(0)
    @Max(1)
    private Integer status;
}
