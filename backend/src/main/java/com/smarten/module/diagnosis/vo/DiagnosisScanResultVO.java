package com.smarten.module.diagnosis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "诊断扫描结果")
public class DiagnosisScanResultVO {

    private String batchNo;

    private int ruleCount;

    private int deviceCount;

    private int signalCount;

    private int newAnomalies;

    private int mergedAnomalies;

    private int reportCount;

    private long durationMs;

    private boolean skipped;
}
