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
import java.time.LocalDate;

@Data
@Schema(description = "设备保存参数")
public class DeviceDTO {

    @Schema(description = "所属企业，非平台管理员可省略")
    private Long enterpriseId;

    @NotNull(message = "所属产线不能为空")
    private Long lineId;

    @NotBlank(message = "设备编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,64}$", message = "设备编码只能包含字母、数字、下划线、中划线")
    private String code;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 128)
    private String name;

    @NotBlank(message = "设备类型不能为空")
    @Size(max = 64)
    private String deviceType;

    @Size(max = 64)
    private String model;

    @DecimalMin(value = "0", message = "额定功率不能为负数")
    private BigDecimal ratedPower;

    private Long energyTypeId;

    private LocalDate commissionDate;

    @Min(0)
    @Max(3)
    @Schema(description = "1运行 2停机 3检修 0停用")
    private Integer status;

    @Size(max = 255)
    private String remark;
}
