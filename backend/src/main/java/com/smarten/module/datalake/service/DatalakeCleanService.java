package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarten.module.datalake.entity.DataIngestFailure;
import com.smarten.module.datalake.entity.EnergyDetail;
import com.smarten.module.datalake.entity.EnergyRawData;
import com.smarten.module.datalake.mapper.DataIngestFailureMapper;
import com.smarten.module.datalake.mapper.EnergyDetailMapper;
import com.smarten.module.datalake.mapper.EnergyRawDataMapper;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.entity.EnergyType;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import com.smarten.module.masterdata.mapper.EnergyTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DWD 清洗：格式校验、设备校验、单位/倍率换算、累计读数差值、去重与异常标记。
 *
 * <p>原始数据永不删除；清洗结果写入 energy_detail，原始行只更新 clean_status 与 quality。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatalakeCleanService {

    public static final String CALC_VERSION = "DWD-V1";

    private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(1_000_000_000L);

    private final EnergyRawDataMapper rawMapper;
    private final EnergyDetailMapper detailMapper;
    private final EnergyMeterMapper meterMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyTypeMapper energyTypeMapper;
    private final DataIngestFailureMapper failureMapper;

    /** 按批次清洗 */
    @Transactional(rollbackFor = Exception.class)
    public int cleanBatch(String batchNo) {
        List<EnergyRawData> rows = rawMapper.selectList(new LambdaQueryWrapper<EnergyRawData>()
                .eq(EnergyRawData::getBatchNo, batchNo)
                .eq(EnergyRawData::getCleanStatus, 0)
                .orderByAsc(EnergyRawData::getMeterId)
                .orderByAsc(EnergyRawData::getCollectTime));
        return clean(rows);
    }

    /** 按日期清洗待处理数据 */
    @Transactional(rollbackFor = Exception.class)
    public int cleanPending(LocalDate date, int limit) {
        LambdaQueryWrapper<EnergyRawData> wrapper = new LambdaQueryWrapper<EnergyRawData>()
                .eq(EnergyRawData::getCleanStatus, 0)
                .orderByAsc(EnergyRawData::getMeterId)
                .orderByAsc(EnergyRawData::getCollectTime);
        if (date != null) {
            wrapper.ge(EnergyRawData::getCollectTime, date.atStartOfDay())
                    .lt(EnergyRawData::getCollectTime, date.plusDays(1).atStartOfDay());
        }
        wrapper.last("LIMIT " + Math.max(1, Math.min(limit, 20000)));
        return clean(rawMapper.selectList(wrapper));
    }

    private int clean(List<EnergyRawData> rows) {
        Map<Long, EnergyMeter> meterCache = new HashMap<>();
        Map<Long, EnergyType> typeCache = new HashMap<>();
        int cleaned = 0;

        for (EnergyRawData raw : rows) {
            EnergyMeter meter = meterCache.computeIfAbsent(raw.getMeterId(),
                    id -> meterMapper.selectById(id));
            if (meter == null) {
                markIgnored(raw, "METER_NOT_FOUND", "仪表不存在，原始数据保留待人工处理");
                continue;
            }
            if (meter.getStatus() == null || meter.getStatus() != 1) {
                markIgnored(raw, "METER_DISABLED", "仪表已停用");
                continue;
            }
            if (raw.getRawValue() == null || raw.getRawValue().signum() < 0
                    || raw.getRawValue().compareTo(MAX_VALUE) > 0) {
                raw.setQuality(3);
                markIgnored(raw, "VALUE_OUT_OF_RANGE", "读数越界或为空");
                continue;
            }

            EnergyType type = typeCache.computeIfAbsent(meter.getEnergyTypeId(),
                    id -> energyTypeMapper.selectById(id));
            BigDecimal ratio = meter.getRatio() == null ? BigDecimal.ONE : meter.getRatio();
            int readingType = raw.getReadingType() == null ? 1 : raw.getReadingType();

            BigDecimal energy;
            BigDecimal startReading = null;
            BigDecimal endReading = null;
            LocalDateTime periodStart;
            LocalDateTime periodEnd;

            if (readingType == 2) {
                // 周期增量：直接使用读数
                energy = raw.getRawValue().multiply(ratio);
                periodEnd = raw.getCollectTime();
                periodStart = periodEnd.minusSeconds(intervalSeconds(meter));
            } else {
                // 累计读数：与上一条已清洗读数做差
                EnergyRawData previous = rawMapper.selectOne(new LambdaQueryWrapper<EnergyRawData>()
                        .eq(EnergyRawData::getMeterId, raw.getMeterId())
                        .eq(EnergyRawData::getCleanStatus, 1)
                        .lt(EnergyRawData::getCollectTime, raw.getCollectTime())
                        .orderByDesc(EnergyRawData::getCollectTime)
                        .last("LIMIT 1"));
                if (previous == null) {
                    // 首个读数作为基线，不产生周期能耗
                    raw.setCleanStatus(1);
                    rawMapper.updateById(raw);
                    cleaned++;
                    continue;
                }
                BigDecimal delta = raw.getRawValue().subtract(previous.getRawValue());
                if (delta.signum() < 0) {
                    // 表计清零/换表：从 0 起算，并标记异常
                    delta = raw.getRawValue();
                    raw.setQuality(2);
                }
                energy = delta.multiply(ratio);
                startReading = previous.getRawValue();
                endReading = raw.getRawValue();
                periodStart = previous.getCollectTime();
                periodEnd = raw.getCollectTime();

                if (raw.getQuality() == null || raw.getQuality() == 1) {
                    if (isJump(ratedPower(meter), energy, intervalSeconds(meter))) {
                        raw.setQuality(2);
                    }
                }
            }

            if (energy.signum() < 0) {
                energy = BigDecimal.ZERO;
            }

            EnergyDetail detail = new EnergyDetail();
            detail.setEnterpriseId(raw.getEnterpriseId());
            detail.setMeterId(raw.getMeterId());
            detail.setDeviceId(meter.getDeviceId());
            detail.setLineId(meter.getLineId());
            detail.setEnergyTypeId(meter.getEnergyTypeId());
            detail.setPeriodStart(periodStart);
            detail.setPeriodEnd(periodEnd);
            detail.setEnergyValue(energy.setScale(4, RoundingMode.HALF_UP));
            detail.setUnit(type == null ? null : type.getUnit());
            detail.setStartReading(startReading);
            detail.setEndReading(endReading);
            detail.setEstimated(0);
            detail.setQualityFlag(raw.getQuality() == null ? 1 : raw.getQuality());
            detail.setSourceCount(1);
            detail.setCalcVersion(CALC_VERSION);

            try {
                detailMapper.insert(detail);
            } catch (DuplicateKeyException ex) {
                // 明细已存在：视为重复数据，原始数据保留并标记已清洗
                raw.setQuality(4);
                raw.setCleanStatus(1);
                rawMapper.updateById(raw);
                continue;
            }

            raw.setCleanStatus(1);
            rawMapper.updateById(raw);
            cleaned++;
        }
        return cleaned;
    }

    private BigDecimal ratedPower(EnergyMeter meter) {
        if (meter.getDeviceId() == null) {
            return null;
        }
        EnergyDevice device = deviceMapper.selectById(meter.getDeviceId());
        return device == null ? null : device.getRatedPower();
    }

    private boolean isJump(BigDecimal ratedPower, BigDecimal energy, int intervalSeconds) {
        if (ratedPower == null || ratedPower.signum() <= 0) {
            return false;
        }
        BigDecimal hours = BigDecimal.valueOf(intervalSeconds).divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
        BigDecimal threshold = ratedPower.multiply(hours).multiply(BigDecimal.valueOf(3));
        return energy.compareTo(threshold) > 0;
    }

    private int intervalSeconds(EnergyMeter meter) {
        return meter.getCollectIntervalSeconds() == null ? 60 : meter.getCollectIntervalSeconds();
    }

    private void markIgnored(EnergyRawData raw, String code, String message) {
        raw.setCleanStatus(2);
        rawMapper.updateById(raw);
        DataIngestFailure failure = new DataIngestFailure();
        failure.setBatchNo(raw.getBatchNo());
        failure.setEnterpriseId(raw.getEnterpriseId());
        failure.setMeterId(raw.getMeterId());
        failure.setCollectTime(raw.getCollectTime());
        failure.setPayload(String.valueOf(raw.getRawValue()));
        failure.setErrorCode(code);
        failure.setErrorMsg(message);
        failure.setRetryCount(0);
        failure.setStatus(0);
        failureMapper.insert(failure);
    }
}
