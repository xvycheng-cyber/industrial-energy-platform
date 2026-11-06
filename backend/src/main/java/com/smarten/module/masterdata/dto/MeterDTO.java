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
import java.time.LocalDateTime;

@Data
@Schema(description = "计量点保存参数")
public class MeterDTO {

    @Schema(description = "所属企业，非平台管理员可省略")
    private Long enterpriseId;

    @Schema(description = "绑定设备；与产线二选一")
    private Long deviceId;

    @Schema(description = "绑定产线（总表）")
    private Long lineId;

    @NotBlank(message = "仪表编号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,64}$", message = "仪表编号只能包含字母、数字、下划线、中划线")
    private String meterCode;

    @NotBlank(message = "仪表名称不能为空")
    @Size(max = 128)
    private String meterName;

    @NotNull(message = "能源类型不能为空")
    private Long energyTypeId;

    @DecimalMin(value = "0.0001", message = "倍率必须大于 0")
    private BigDecimal ratio;

    private LocalDateTime installTime;

    @Min(0)
    @Max(1)
    private Integer status;

    @Size(max = 255)
    private String remark;
}
