package com.smarten.module.rectification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务分配/转派")
public class TaskAssignRequest {

    @NotNull(message = "负责人不能为空")
    private Long assigneeId;

    private LocalDateTime deadline;

    private String reason;
}
