package com.smarten.module.datalake.service;

import com.smarten.module.datalake.mapper.EnergyAggregateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DWS 聚合：5 分钟 / 15 分钟 / 小时 / 日 / 月，企业、车间、产线、设备 × 能源类型。
 *
 * <p>按「完整时间桶」重算并 upsert，保证重复执行结果一致。</p>
 */
@Service
@RequiredArgsConstructor
public class DatalakeAggregateService {

    public static final String CALC_VERSION = "DWS-V1";

    public static final int PERIOD_5M = 4;
    public static final int PERIOD_15M = 5;
    public static final int PERIOD_HOUR = 1;
    public static final int PERIOD_DAY = 2;
    public static final int PERIOD_MONTH = 3;

    private static final Map<Integer, String> BUCKETS = new LinkedHashMap<>();

    static {
        BUCKETS.put(PERIOD_5M, "FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(d.period_start) / 300) * 300)");
        BUCKETS.put(PERIOD_15M, "FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(d.period_start) / 900) * 900)");
        BUCKETS.put(PERIOD_HOUR, "DATE_FORMAT(d.period_start, '%Y-%m-%d %H:00:00')");
        BUCKETS.put(PERIOD_DAY, "DATE_FORMAT(d.period_start, '%Y-%m-%d 00:00:00')");
        BUCKETS.put(PERIOD_MONTH, "DATE_FORMAT(d.period_start, '%Y-%m-01 00:00:00')");
    }

    private final EnergyAggregateMapper aggregateMapper;

    /** 聚合给定时间范围内覆盖到的完整周期桶（全部周期） */
    @Transactional(rollbackFor = Exception.class)
    public int aggregateRange(LocalDateTime start, LocalDateTime end) {
        return aggregateRange(start, end, BUCKETS.keySet());
    }

    /** 聚合指定周期类型（用于按需重算） */
    @Transactional(rollbackFor = Exception.class)
    public int aggregateRange(LocalDateTime start, LocalDateTime end, java.util.Collection<Integer> periodTypes) {
        int affected = 0;
        for (Map.Entry<Integer, String> entry : BUCKETS.entrySet()) {
            if (periodTypes != null && !periodTypes.contains(entry.getKey())) {
                continue;
            }
            int periodType = entry.getKey();
            LocalDateTime bucketStart = floor(periodType, start);
            while (bucketStart.isBefore(end)) {
                LocalDateTime bucketEnd = bucketStart.plus(span(periodType));
                affected += runBucket(periodType, entry.getValue(), bucketStart, bucketEnd);
                bucketStart = bucketEnd;
            }
        }
        return affected;
    }

    public int aggregateDay(LocalDate date) {
        return aggregateRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    private int runBucket(int periodType, String bucket, LocalDateTime start, LocalDateTime end) {
        int affected = 0;
        affected += aggregateMapper.aggregateDevice(periodType, bucket, start, end, CALC_VERSION);
        affected += aggregateMapper.aggregateLine(periodType, bucket, start, end, CALC_VERSION);
        affected += aggregateMapper.aggregateWorkshop(periodType, bucket, start, end, CALC_VERSION);
        affected += aggregateMapper.aggregateEnterprise(periodType, bucket, start, end, CALC_VERSION);
        return affected;
    }

    private LocalDateTime floor(int periodType, LocalDateTime time) {
        return switch (periodType) {
            case PERIOD_5M -> time.truncatedTo(ChronoUnit.MINUTES)
                    .withMinute(time.getMinute() / 5 * 5).withSecond(0).withNano(0);
            case PERIOD_15M -> time.truncatedTo(ChronoUnit.MINUTES)
                    .withMinute(time.getMinute() / 15 * 15).withSecond(0).withNano(0);
            case PERIOD_HOUR -> time.truncatedTo(ChronoUnit.HOURS);
            case PERIOD_DAY -> time.toLocalDate().atStartOfDay();
            case PERIOD_MONTH -> time.toLocalDate().withDayOfMonth(1).atStartOfDay();
            default -> time;
        };
    }

    private ChronoUnit unit(int periodType) {
        return switch (periodType) {
            case PERIOD_5M, PERIOD_15M -> ChronoUnit.MINUTES;
            case PERIOD_HOUR -> ChronoUnit.HOURS;
            case PERIOD_DAY -> ChronoUnit.DAYS;
            case PERIOD_MONTH -> ChronoUnit.MONTHS;
            default -> ChronoUnit.MINUTES;
        };
    }

    private java.time.temporal.TemporalAmount span(int periodType) {
        return switch (periodType) {
            case PERIOD_5M -> java.time.Duration.ofMinutes(5);
            case PERIOD_15M -> java.time.Duration.ofMinutes(15);
            case PERIOD_HOUR -> java.time.Duration.ofHours(1);
            case PERIOD_DAY -> java.time.Duration.ofDays(1);
            case PERIOD_MONTH -> java.time.Period.ofMonths(1);
            default -> java.time.Duration.ofMinutes(1);
        };
    }
}
