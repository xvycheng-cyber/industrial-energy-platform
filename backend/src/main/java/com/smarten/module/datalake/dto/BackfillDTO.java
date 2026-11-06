package com.smarten.module.datalake.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "缺口补录参数")
public class BackfillDTO {

    @NotNull(message = "缺口 ID 不能为空")
    private Long gapId;
}
