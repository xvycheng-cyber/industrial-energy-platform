package com.smarten.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通用状态变更参数。
 */
@Data
@Schema(description = "状态变更参数")
public class StatusDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 3, message = "状态取值不合法")
    @Schema(description = "状态值：启用类 1/0；设备 1运行 2停机 3检修 0停用", example = "1")
    private Integer status;
}
