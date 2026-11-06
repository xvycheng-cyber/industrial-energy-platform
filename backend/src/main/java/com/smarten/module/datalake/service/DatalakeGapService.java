package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.datalake.entity.EnergyDataGap;
import com.smarten.module.datalake.entity.EnergyRawData;
import com.smarten.module.datalake.mapper.EnergyDataGapMapper;
import com.smarten.module.datalake.mapper.EnergyRawDataMapper;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 缺失检测与补录。
 *
 * <p>检测按仪表采集间隔逐点比对；补录写入带 {@code BACKFILL} 来源与估算标记的数据，
 * 不删除、不覆盖任何原始数据。</p>
 */
@Service
@RequiredArgsConstructor
public class DatalakeGapService {

    private final EnergyMeterMapper meterMapper;
    private final EnergyRawDataMapper rawMapper;
    private final EnergyDataGapMapper gapMapper;
    private final DatalakeCleanService cleanService;
    private final DataScopeHelper dataScope;

    /** 检测指定日期的数据缺口，返回新增缺口数量 */
    @Transactional(rollbackFor = Exception.class)
    public int detect(LocalDate date, Long enterpriseId) {
        Long scope = dataScope.resolveEnterpriseId(enterpriseId);
        LambdaQueryWrapper<EnergyMeter> wrapper = new LambdaQueryWrapper<EnergyMeter>()
                .eq(EnergyMeter::getStatus, 1);
        if (scope != null) {
            wrapper.eq(EnergyMeter::getEnterpriseId, scope);
        }
        List<EnergyMeter> meters = meterMapper.selectList(wrapper);

        int created = 0;
        for (EnergyMeter meter : meters) {
            int interval = meter.getCollectIntervalSeconds() == null ? 60 : meter.getCollectIntervalSeconds();
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<EnergyRawData> rows = rawMapper.selectList(new LambdaQueryWrapper<EnergyRawData>()
                    .select(EnergyRawData::getCollectTime)
                    .eq(EnergyRawData::getMeterId, meter.getId())
                    .ge(EnergyRawData::getCollectTime, dayStart)
                    .lt(EnergyRawData::getCollectTime, dayEnd));
            Set<LocalDateTime> actual = new HashSet<>();
            for (EnergyRawData row : rows) {
                actual.add(row.getCollectTime());
            }

            List<LocalDateTime> missing = new ArrayList<>();
            for (LocalDateTime t = dayStart; t.isBefore(dayEnd); t = t.plusSeconds(interval)) {
                if (!actual.contains(t)) {
                    missing.add(t);
                }
            }
            if (missing.isEmpty()) {
                continue;
            }

            // 连续缺失合并为一个缺口区间
            LocalDateTime rangeStart = missing.get(0);
            LocalDateTime previous = missing.get(0);
            for (int i = 1; i <= missing.size(); i++) {
                LocalDateTime current = i < missing.size() ? missing.get(i) : null;
                boolean contiguous = current != null && current.equals(previous.plusSeconds(interval));
                if (!contiguous) {
                    LocalDateTime rangeEnd = previous.plusSeconds(interval);
                    if (rangeEnd.isAfter(dayEnd)) {
                        rangeEnd = dayEnd;
                    }
                    Long exists = gapMapper.selectCount(new LambdaQueryWrapper<EnergyDataGap>()
                            .eq(EnergyDataGap::getMeterId, meter.getId())
                            .eq(EnergyDataGap::getGapStart, rangeStart));
                    if (exists == null || exists == 0) {
                        EnergyDataGap gap = new EnergyDataGap();
                        gap.setEnterpriseId(meter.getEnterpriseId());
                        gap.setMeterId(meter.getId());
                        gap.setEnergyTypeId(meter.getEnergyTypeId());
                        gap.setGapStart(rangeStart);
                        gap.setGapEnd(rangeEnd);
                        int expected = (int) ((rangeEnd.toEpochSecond(java.time.ZoneOffset.UTC)
                                - rangeStart.toEpochSecond(java.time.ZoneOffset.UTC)) / interval);
                        gap.setExpectedCount(Math.max(1, expected));
                        gap.setActualCount(0);
                        gap.setMissingCount(Math.max(1, expected));
                        gap.setStatus(0);
                        gapMapper.insert(gap);
                        created++;
                    }
                    if (current == null) {
                        break;
                    }
                    rangeStart = current;
                }
                previous = current == null ? previous : current;
            }
        }
        return created;
    }

    /** 对缺口补录估算数据 */
    @Transactional(rollbackFor = Exception.class)
    public int backfill(Long gapId) {
        EnergyDataGap gap = gapMapper.selectById(gapId);
        if (gap == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "缺口不存在");
        }
        dataScope.checkAccessible(gap.getEnterpriseId());
        if (gap.getStatus() != null && gap.getStatus() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该缺口已补录");
        }

        EnergyMeter meter = meterMapper.selectById(gap.getMeterId());
        if (meter == null) {
            throw new BusinessException(ResultCode.METER_NOT_FOUND);
        }
        int interval = meter.getCollectIntervalSeconds() == null ? 60 : meter.getCollectIntervalSeconds();
        BigDecimal ratio = meter.getRatio() == null ? BigDecimal.ONE : meter.getRatio();

        List<EnergyRawData> previousRows = rawMapper.selectList(new LambdaQueryWrapper<EnergyRawData>()
                .eq(EnergyRawData::getMeterId, meter.getId())
                .lt(EnergyRawData::getCollectTime, gap.getGapStart())
                .orderByDesc(EnergyRawData::getCollectTime)
                .last("LIMIT 20"));
        BigDecimal lastValue = previousRows.isEmpty() ? BigDecimal.ZERO : previousRows.get(0).getRawValue();
        BigDecimal avgDelta = averageDelta(previousRows);

        String batchNo = "BF-" + gap.getId() + "-" + System.currentTimeMillis();
        int inserted = 0;
        BigDecimal value = lastValue;
        for (LocalDateTime t = gap.getGapStart(); t.isBefore(gap.getGapEnd()); t = t.plusSeconds(interval)) {
            int readingType = meter.getReadingType() == null ? 1 : meter.getReadingType();
            BigDecimal rawValue;
            if (readingType == 2) {
                rawValue = avgDelta.max(BigDecimal.ZERO);
            } else {
                value = value.add(avgDelta.max(BigDecimal.ZERO));
                rawValue = value;
            }
            EnergyRawData raw = new EnergyRawData();
            raw.setEnterpriseId(meter.getEnterpriseId());
            raw.setMeterId(meter.getId());
            raw.setDeviceId(meter.getDeviceId());
            raw.setCollectTime(t);
            raw.setPointCode("TOTAL");
            raw.setRawValue(rawValue.setScale(4, RoundingMode.HALF_UP));
            raw.setReadingType(readingType);
            raw.setReceivedTime(LocalDateTime.now());
            raw.setQuality(2);
            raw.setSource("BACKFILL");
            raw.setBatchNo(batchNo);
            raw.setCleanStatus(0);
            try {
                rawMapper.insert(raw);
                inserted++;
            } catch (org.springframework.dao.DuplicateKeyException ignored) {
                // 该时刻已有数据，跳过
            }
        }

        if (inserted > 0) {
            cleanService.cleanBatch(batchNo);
        }
        gap.setStatus(1);
        gap.setFillTime(LocalDateTime.now());
        gap.setActualCount(inserted);
        gap.setMissingCount(Math.max(0, gap.getExpectedCount() - inserted));
        gapMapper.updateById(gap);
        return inserted;
    }

    private BigDecimal averageDelta(List<EnergyRawData> rows) {
        if (rows.size() < 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (int i = 1; i < rows.size(); i++) {
            BigDecimal delta = rows.get(i - 1).getRawValue().subtract(rows.get(i).getRawValue());
            if (delta.signum() > 0) {
                total = total.add(delta);
                count++;
            }
        }
        return count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }
}
