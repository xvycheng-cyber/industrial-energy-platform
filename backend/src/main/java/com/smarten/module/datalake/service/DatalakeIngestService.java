package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.datalake.dto.IngestBatchDTO;
import com.smarten.module.datalake.dto.ReadingDTO;
import com.smarten.module.datalake.entity.DataIngestFailure;
import com.smarten.module.datalake.entity.DataIngestionLog;
import com.smarten.module.datalake.entity.EnergyRawData;
import com.smarten.module.datalake.mapper.DataIngestFailureMapper;
import com.smarten.module.datalake.mapper.DataIngestionLogMapper;
import com.smarten.module.datalake.mapper.EnergyRawDataMapper;
import com.smarten.module.datalake.vo.IngestResultVO;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ODS 接入：批量入库、幂等控制、失败记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatalakeIngestService {

    private final EnergyRawDataMapper rawMapper;
    private final EnergyMeterMapper meterMapper;
    private final DataIngestionLogMapper logMapper;
    private final DataIngestFailureMapper failureMapper;
    private final DataScopeHelper dataScope;

    /**
     * 批量接入。批次号是幂等键：已成功完成的批次重复提交直接返回原结果，不会重复入库。
     */
    @Transactional(rollbackFor = Exception.class)
    public IngestResultVO ingest(IngestBatchDTO dto) {
        return doIngest(dto, true);
    }

    /**
     * 系统内部接入（定时采集、补录），跳过当前登录用户的数据权限校验。
     */
    @Transactional(rollbackFor = Exception.class)
    public IngestResultVO ingestSystem(IngestBatchDTO dto) {
        return doIngest(dto, false);
    }

    private IngestResultVO doIngest(IngestBatchDTO dto, boolean checkScope) {
        DataIngestionLog existing = logMapper.selectOne(new LambdaQueryWrapper<DataIngestionLog>()
                .eq(DataIngestionLog::getBatchNo, dto.getBatchNo()));
        if (existing != null && existing.getStatus() != null && existing.getStatus() == 1) {
            return toResult(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        DataIngestionLog ingestionLog = existing == null ? new DataIngestionLog() : existing;
        ingestionLog.setBatchNo(dto.getBatchNo());
        ingestionLog.setSource(dto.getSource());
        ingestionLog.setStartTime(now);
        ingestionLog.setStatus(0);
        if (dto.getReadings() != null && !dto.getReadings().isEmpty()) {
            LocalDate dataDate = dto.getReadings().get(0).getCollectTime().toLocalDate();
            ingestionLog.setDataDate(dataDate);
        }
        if (existing == null) {
            logMapper.insert(ingestionLog);
        } else {
            logMapper.updateById(ingestionLog);
        }

        int received = dto.getReadings() == null ? 0 : dto.getReadings().size();
        int inserted = 0;
        int duplicated = 0;
        int failed = 0;
        Long enterpriseId = null;

        for (ReadingDTO reading : dto.getReadings()) {
            try {
                EnergyMeter meter = meterMapper.selectById(reading.getMeterId());
                if (meter == null) {
                    failed++;
                    recordFailure(dto.getBatchNo(), null, reading, "METER_NOT_FOUND", "仪表不存在");
                    continue;
                }
                if (meter.getStatus() == null || meter.getStatus() != 1) {
                    failed++;
                    recordFailure(dto.getBatchNo(), meter.getEnterpriseId(), reading, "METER_DISABLED", "仪表已停用");
                    continue;
                }
                if (checkScope) {
                    dataScope.checkAccessible(meter.getEnterpriseId());
                }
                enterpriseId = meter.getEnterpriseId();

                EnergyRawData raw = new EnergyRawData();
                raw.setEnterpriseId(meter.getEnterpriseId());
                raw.setMeterId(meter.getId());
                raw.setDeviceId(meter.getDeviceId());
                raw.setCollectTime(reading.getCollectTime());
                raw.setPointCode(reading.getPointCode() == null || reading.getPointCode().isBlank()
                        ? "TOTAL" : reading.getPointCode());
                raw.setRawValue(reading.getValue());
                raw.setReadingType(reading.getReadingType() != null ? reading.getReadingType() : meter.getReadingType());
                raw.setReceivedTime(now);
                raw.setQuality(1);
                raw.setSource(dto.getSource());
                raw.setBatchNo(dto.getBatchNo());
                raw.setCleanStatus(0);
                rawMapper.insert(raw);
                inserted++;
            } catch (DuplicateKeyException ex) {
                duplicated++;
            } catch (BusinessException ex) {
                failed++;
                recordFailure(dto.getBatchNo(), null, reading, "FORBIDDEN", ex.getMessage());
            } catch (Exception ex) {
                failed++;
                recordFailure(dto.getBatchNo(), null, reading, "INGEST_ERROR", truncate(ex.getMessage()));
                log.warn("接入失败 batch={} meter={}", dto.getBatchNo(), reading.getMeterId(), ex);
            }
        }

        ingestionLog.setEnterpriseId(enterpriseId);
        ingestionLog.setExpectedCount(received);
        ingestionLog.setReceivedCount(received);
        ingestionLog.setValidCount(inserted);
        ingestionLog.setMissingCount(Math.max(0, received - inserted));
        ingestionLog.setSuccessRate(rate(inserted, received));
        ingestionLog.setStatus(failed == 0 && duplicated == 0 ? 1 : (inserted > 0 ? 2 : 3));
        ingestionLog.setEndTime(LocalDateTime.now());
        logMapper.updateById(ingestionLog);

        return toResult(ingestionLog);
    }

    private void recordFailure(String batchNo, Long enterpriseId, ReadingDTO reading, String code, String message) {
        DataIngestFailure failure = new DataIngestFailure();
        failure.setBatchNo(batchNo);
        failure.setEnterpriseId(enterpriseId);
        failure.setMeterId(reading.getMeterId());
        failure.setCollectTime(reading.getCollectTime());
        failure.setPayload(String.valueOf(reading.getValue()));
        failure.setErrorCode(code);
        failure.setErrorMsg(message);
        failure.setRetryCount(0);
        failure.setStatus(0);
        failureMapper.insert(failure);
    }

    private BigDecimal rate(int valid, int expected) {
        if (expected <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(valid * 100.0 / expected).setScale(2, RoundingMode.HALF_UP);
    }

    private String truncate(String message) {
        if (message == null) {
            return "未知错误";
        }
        return message.length() > 400 ? message.substring(0, 400) : message;
    }

    private IngestResultVO toResult(DataIngestionLog logEntity) {
        IngestResultVO vo = new IngestResultVO();
        vo.setBatchNo(logEntity.getBatchNo());
        vo.setReceivedCount(logEntity.getReceivedCount() == null ? 0 : logEntity.getReceivedCount());
        vo.setInsertedCount(logEntity.getValidCount() == null ? 0 : logEntity.getValidCount());
        vo.setDuplicatedCount(0);
        vo.setFailedCount(Math.max(0, vo.getReceivedCount() - vo.getInsertedCount()));
        vo.setExpectedCount(logEntity.getExpectedCount() == null ? 0 : logEntity.getExpectedCount());
        vo.setCompletenessRate(logEntity.getSuccessRate() == null ? BigDecimal.ZERO : logEntity.getSuccessRate());
        vo.setStatus(logEntity.getStatus() == null ? 3 : logEntity.getStatus());
        return vo;
    }
}
