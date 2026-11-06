package com.smarten.module.saving.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "节能量趋势与排名")
public class SavingSummaryVO {

    private BigDecimal totalSaving = BigDecimal.ZERO;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private long resultCount;

    private List<RankItem> ranking = new ArrayList<>();

    private List<TrendItem> trend = new ArrayList<>();

    @Data
    public static class RankItem {
        private Long deviceId;
        private String deviceName;
        private BigDecimal saving;
        private BigDecimal rate;
    }

    @Data
    public static class TrendItem {
        private String month;
        private BigDecimal saving;
        private BigDecimal amount;
    }
}
