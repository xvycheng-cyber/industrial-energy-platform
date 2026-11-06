package com.smarten.module.diagnosis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "扫描请求")
public class DiagnosisScanRequest {

    private Long enterpriseId;

    private LocalDateTime start;

    private LocalDateTime end;
}
