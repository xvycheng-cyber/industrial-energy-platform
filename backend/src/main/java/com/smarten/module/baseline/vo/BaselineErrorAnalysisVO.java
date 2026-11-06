package com.smarten.module.baseline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "模型误差分析")
public class BaselineErrorAnalysisVO {

    private Long baselineId;

    private String modelVersion;

    private Integer sampleCount;

    private BigDecimal mae;

    private BigDecimal rmse;

    private BigDecimal mape;

    private BigDecimal r2;

    private Integer fallbackCount;

    private List<ResidualBucket> buckets = new ArrayList<>();

    private List<BaselineRecordVO> worst = new ArrayList<>();

    @Data
    @Schema(description = "残差分布桶")
    public static class ResidualBucket {

        private String range;

        private Integer count;

        public ResidualBucket() {
        }

        public ResidualBucket(String range, Integer count) {
            this.range = range;
            this.count = count;
        }
    }
}
