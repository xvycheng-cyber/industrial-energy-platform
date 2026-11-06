package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.module.datalake.dto.AggregateQueryDTO;
import com.smarten.module.datalake.dto.DetailQueryDTO;
import com.smarten.module.datalake.dto.LogQueryDTO;
import com.smarten.module.datalake.dto.RawQueryDTO;
import com.smarten.module.datalake.entity.DataIngestFailure;
import com.smarten.module.datalake.entity.DataIngestionLog;
import com.smarten.module.datalake.entity.EnergyAggregate;
import com.smarten.module.datalake.entity.EnergyDetail;
import com.smarten.module.datalake.entity.EnergyRawData;
import com.smarten.module.datalake.mapper.DataIngestFailureMapper;
import com.smarten.module.datalake.mapper.DataIngestionLogMapper;
import com.smarten.module.datalake.mapper.EnergyAggregateMapper;
import com.smarten.module.datalake.mapper.EnergyDetailMapper;
import com.smarten.module.datalake.mapper.EnergyRawDataMapper;
import com.smarten.module.datalake.vo.AggregateVO;
import com.smarten.module.datalake.vo.DetailVO;
import com.smarten.module.datalake.vo.FailureVO;
import com.smarten.module.datalake.vo.LogVO;
import com.smarten.module.datalake.vo.RawDataVO;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyEnterprise;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.entity.EnergyType;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyEnterpriseMapper;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import com.smarten.module.masterdata.mapper.EnergyTypeMapper;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据湖查询：原始数据、明细、聚合、日志、失败记录。
 */
@Service
@RequiredArgsConstructor
public class DatalakeQueryService {

    private final EnergyRawDataMapper rawMapper;
    private final EnergyDetailMapper detailMapper;
    private final EnergyAggregateMapper aggregateMapper;
    private final DataIngestionLogMapper logMapper;
    private final DataIngestFailureMapper failureMapper;
    private final com.smarten.module.datalake.mapper.EnergyDataGapMapper gapMapper;
    private final EnergyMeterMapper meterMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyEnterpriseMapper enterpriseMapper;
    private final EnergyTypeMapper energyTypeMapper;
    private final DataScopeHelper dataScope;

    public PageResult<RawDataVO> rawPage(RawQueryDTO query) {
        LambdaQueryWrapper<EnergyRawData> wrapper = new LambdaQueryWrapper<>();
        if (query.getMeterId() != null) {
            wrapper.eq(EnergyRawData::getMeterId, query.getMeterId());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(EnergyRawData::getDeviceId, query.getDeviceId());
        }
        if (query.getQuality() != null) {
            wrapper.eq(EnergyRawData::getQuality, query.getQuality());
        }
        if (StringUtils.hasText(query.getSource())) {
            wrapper.eq(EnergyRawData::getSource, query.getSource());
        }
        if (StringUtils.hasText(query.getBatchNo())) {
            wrapper.eq(EnergyRawData::getBatchNo, query.getBatchNo());
        }
        if (query.getCleanStatus() != null) {
            wrapper.eq(EnergyRawData::getCleanStatus, query.getCleanStatus());
        }
        if (query.getStart() != null) {
            wrapper.ge(EnergyRawData::getCollectTime, query.getStart());
        }
        if (query.getEnd() != null) {
            wrapper.lt(EnergyRawData::getCollectTime, query.getEnd());
        }
        applyEnterprise(wrapper, query.getEnterpriseId(), EnergyRawData::getEnterpriseId);
        wrapper.orderByDesc(EnergyRawData::getCollectTime);

        Page<EnergyRawData> page = rawMapper.selectPage(query.toPage(), wrapper);
        Map<Long, EnergyMeter> meters = meters(page.getRecords().stream().map(EnergyRawData::getMeterId).toList());
        Map<Long, String> devices = deviceNames(page.getRecords().stream().map(EnergyRawData::getDeviceId).toList());

        List<RawDataVO> records = page.getRecords().stream().map(raw -> {
            RawDataVO vo = new RawDataVO();
            vo.setId(raw.getId());
            vo.setMeterId(raw.getMeterId());
            EnergyMeter meter = meters.get(raw.getMeterId());
            vo.setMeterCode(meter == null ? null : meter.getMeterCode());
            vo.setMeterName(meter == null ? null : meter.getMeterName());
            vo.setDeviceName(raw.getDeviceId() == null ? null : devices.get(raw.getDeviceId()));
            vo.setCollectTime(raw.getCollectTime());
            vo.setRawValue(raw.getRawValue());
            vo.setRawUnit(raw.getRawUnit());
            vo.setReadingType(raw.getReadingType());
            vo.setQuality(raw.getQuality());
            vo.setSource(raw.getSource());
            vo.setBatchNo(raw.getBatchNo());
            vo.setCleanStatus(raw.getCleanStatus());
            vo.setReceivedTime(raw.getReceivedTime());
            return vo;
        }).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<DetailVO> detailPage(DetailQueryDTO query) {
        LambdaQueryWrapper<EnergyDetail> wrapper = new LambdaQueryWrapper<>();
        if (query.getMeterId() != null) {
            wrapper.eq(EnergyDetail::getMeterId, query.getMeterId());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(EnergyDetail::getDeviceId, query.getDeviceId());
        }
        if (query.getQualityFlag() != null) {
            wrapper.eq(EnergyDetail::getQualityFlag, query.getQualityFlag());
        }
        if (query.getStart() != null) {
            wrapper.ge(EnergyDetail::getPeriodStart, query.getStart());
        }
        if (query.getEnd() != null) {
            wrapper.lt(EnergyDetail::getPeriodStart, query.getEnd());
        }
        applyEnterprise(wrapper, query.getEnterpriseId(), EnergyDetail::getEnterpriseId);
        wrapper.orderByDesc(EnergyDetail::getPeriodStart);

        Page<EnergyDetail> page = detailMapper.selectPage(query.toPage(), wrapper);
        Map<Long, EnergyMeter> meters = meters(page.getRecords().stream().map(EnergyDetail::getMeterId).toList());
        Map<Long, String> devices = deviceNames(page.getRecords().stream().map(EnergyDetail::getDeviceId).toList());

        List<DetailVO> records = page.getRecords().stream().map(detail -> {
            DetailVO vo = new DetailVO();
            vo.setId(detail.getId());
            vo.setMeterId(detail.getMeterId());
            EnergyMeter meter = meters.get(detail.getMeterId());
            vo.setMeterName(meter == null ? null : meter.getMeterName());
            vo.setDeviceName(detail.getDeviceId() == null ? null : devices.get(detail.getDeviceId()));
            vo.setPeriodStart(detail.getPeriodStart());
            vo.setPeriodEnd(detail.getPeriodEnd());
            vo.setEnergyValue(detail.getEnergyValue());
            vo.setUnit(detail.getUnit());
            vo.setStartReading(detail.getStartReading());
            vo.setEndReading(detail.getEndReading());
            vo.setEstimated(detail.getEstimated());
            vo.setQualityFlag(detail.getQualityFlag());
            vo.setCalcVersion(detail.getCalcVersion());
            return vo;
        }).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<LogVO> logPage(LogQueryDTO query) {
        LambdaQueryWrapper<DataIngestionLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getBatchNo())) {
            wrapper.like(DataIngestionLog::getBatchNo, query.getBatchNo().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(DataIngestionLog::getStatus, query.getStatus());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(DataIngestionLog::getDataDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(DataIngestionLog::getDataDate, query.getEndDate());
        }
        applyEnterprise(wrapper, query.getEnterpriseId(), DataIngestionLog::getEnterpriseId);
        wrapper.orderByDesc(DataIngestionLog::getId);

        Page<DataIngestionLog> page = logMapper.selectPage(query.toPage(), wrapper);
        Set<Long> enterpriseIds = page.getRecords().stream()
                .map(DataIngestionLog::getEnterpriseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> enterpriseNames = enterpriseIds.isEmpty() ? Collections.emptyMap()
                : enterpriseMapper.selectBatchIds(enterpriseIds).stream()
                .collect(Collectors.toMap(EnergyEnterprise::getId, EnergyEnterprise::getName, (a, b) -> a));

        List<LogVO> records = page.getRecords().stream().map(log -> {
            LogVO vo = new LogVO();
            vo.setId(log.getId());
            vo.setBatchNo(log.getBatchNo());
            vo.setEnterpriseId(log.getEnterpriseId());
            vo.setEnterpriseName(log.getEnterpriseId() == null ? null : enterpriseNames.get(log.getEnterpriseId()));
            vo.setDataDate(log.getDataDate());
            vo.setSource(log.getSource());
            vo.setExpectedCount(log.getExpectedCount());
            vo.setReceivedCount(log.getReceivedCount());
            vo.setValidCount(log.getValidCount());
            vo.setMissingCount(log.getMissingCount());
            vo.setSuccessRate(log.getSuccessRate());
            vo.setStatus(log.getStatus());
            vo.setErrorMsg(log.getErrorMsg());
            vo.setStartTime(log.getStartTime());
            vo.setEndTime(log.getEndTime());
            return vo;
        }).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<FailureVO> failurePage(com.smarten.common.page.PageQuery query, Integer status, Long meterId) {
        LambdaQueryWrapper<DataIngestFailure> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(DataIngestFailure::getStatus, status);
        }
        if (meterId != null) {
            wrapper.eq(DataIngestFailure::getMeterId, meterId);
        }
        wrapper.orderByDesc(DataIngestFailure::getId);
        Page<DataIngestFailure> page = failureMapper.selectPage(query.toPage(), wrapper);

        Map<Long, EnergyMeter> meters = meters(page.getRecords().stream().map(DataIngestFailure::getMeterId).toList());
        List<FailureVO> records = page.getRecords().stream().map(failure -> {
            FailureVO vo = new FailureVO();
            vo.setId(failure.getId());
            vo.setBatchNo(failure.getBatchNo());
            vo.setMeterId(failure.getMeterId());
            EnergyMeter meter = meters.get(failure.getMeterId());
            vo.setMeterName(meter == null ? null : meter.getMeterName());
            vo.setCollectTime(failure.getCollectTime());
            vo.setErrorCode(failure.getErrorCode());
            vo.setErrorMsg(failure.getErrorMsg());
            vo.setRetryCount(failure.getRetryCount());
            vo.setStatus(failure.getStatus());
            vo.setPayload(failure.getPayload());
            vo.setCreateTime(failure.getCreateTime());
            return vo;
        }).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<AggregateVO> aggregatePage(AggregateQueryDTO query) {
        LambdaQueryWrapper<EnergyAggregate> wrapper = new LambdaQueryWrapper<>();
        if (query.getDimType() != null) {
            wrapper.eq(EnergyAggregate::getDimType, query.getDimType());
        }
        if (query.getDimId() != null) {
            wrapper.eq(EnergyAggregate::getDimId, query.getDimId());
        }
        if (query.getEnergyTypeId() != null) {
            wrapper.eq(EnergyAggregate::getEnergyTypeId, query.getEnergyTypeId());
        }
        if (query.getPeriodType() != null) {
            wrapper.eq(EnergyAggregate::getPeriodType, query.getPeriodType());
        }
        if (query.getStart() != null) {
            wrapper.ge(EnergyAggregate::getPeriodStart, query.getStart());
        }
        if (query.getEnd() != null) {
            wrapper.lt(EnergyAggregate::getPeriodStart, query.getEnd());
        }
        dataScope.applyScope(wrapper, EnergyAggregate::getEnterpriseId);
        wrapper.orderByDesc(EnergyAggregate::getPeriodStart);

        Page<EnergyAggregate> page = aggregateMapper.selectPage(query.toPage(), wrapper);
        List<Long> energyTypeIds = page.getRecords().stream()
                .map(EnergyAggregate::getEnergyTypeId).distinct().toList();
        Map<Long, EnergyType> types = energyTypeIds.isEmpty() ? Collections.emptyMap()
                : energyTypeMapper.selectBatchIds(energyTypeIds).stream()
                .collect(Collectors.toMap(EnergyType::getId, t -> t, (a, b) -> a));

        List<AggregateVO> records = page.getRecords().stream().map(item -> {
            AggregateVO vo = new AggregateVO();
            vo.setId(item.getId());
            vo.setDimType(item.getDimType());
            vo.setDimId(item.getDimId());
            vo.setEnergyTypeId(item.getEnergyTypeId());
            EnergyType type = types.get(item.getEnergyTypeId());
            vo.setEnergyTypeName(type == null ? null : type.getName());
            vo.setUnit(type == null ? null : type.getUnit());
            vo.setPeriodType(item.getPeriodType());
            vo.setPeriodStart(item.getPeriodStart());
            vo.setEnergyValue(item.getEnergyValue());
            vo.setCostAmount(item.getCostAmount());
            vo.setDataCount(item.getDataCount());
            return vo;
        }).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 数据质量概览：按质量标记与清洗状态统计 */
    public Map<String, Object> qualitySummary(LocalDate date, Long enterpriseId) {
        LocalDate day = date == null ? LocalDate.now() : date;
        Long scope = dataScope.resolveEnterpriseId(enterpriseId);

        java.util.function.Function<Integer, Long> countQuality = quality -> {
            LambdaQueryWrapper<EnergyRawData> wrapper = new LambdaQueryWrapper<EnergyRawData>()
                    .eq(EnergyRawData::getQuality, quality)
                    .ge(EnergyRawData::getCollectTime, day.atStartOfDay())
                    .lt(EnergyRawData::getCollectTime, day.plusDays(1).atStartOfDay());
            if (scope != null) {
                wrapper.eq(EnergyRawData::getEnterpriseId, scope);
            }
            return rawMapper.selectCount(wrapper);
        };

        LambdaQueryWrapper<EnergyRawData> totalWrapper = new LambdaQueryWrapper<EnergyRawData>()
                .ge(EnergyRawData::getCollectTime, day.atStartOfDay())
                .lt(EnergyRawData::getCollectTime, day.plusDays(1).atStartOfDay());
        if (scope != null) {
            totalWrapper.eq(EnergyRawData::getEnterpriseId, scope);
        }
        long total = rawMapper.selectCount(totalWrapper);

        LambdaQueryWrapper<EnergyRawData> pendingWrapper = new LambdaQueryWrapper<EnergyRawData>()
                .eq(EnergyRawData::getCleanStatus, 0)
                .ge(EnergyRawData::getCollectTime, day.atStartOfDay())
                .lt(EnergyRawData::getCollectTime, day.plusDays(1).atStartOfDay());
        if (scope != null) {
            pendingWrapper.eq(EnergyRawData::getEnterpriseId, scope);
        }

        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("date", day.toString());
        summary.put("total", total);
        summary.put("normal", countQuality.apply(1));
        summary.put("suspicious", countQuality.apply(2));
        summary.put("invalid", countQuality.apply(3));
        summary.put("duplicate", countQuality.apply(4));
        summary.put("pendingClean", rawMapper.selectCount(pendingWrapper));
        summary.put("failurePending", failureMapper.selectCount(new LambdaQueryWrapper<DataIngestFailure>()
                .eq(DataIngestFailure::getStatus, 0)));
        return summary;
    }

    /** 缺口分页 */
    public PageResult<com.smarten.module.datalake.vo.GapVO> gapPage(com.smarten.common.page.PageQuery query,
                                                                   Integer status) {
        LambdaQueryWrapper<com.smarten.module.datalake.entity.EnergyDataGap> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(com.smarten.module.datalake.entity.EnergyDataGap::getStatus, status);
        }
        dataScope.applyScope(wrapper, com.smarten.module.datalake.entity.EnergyDataGap::getEnterpriseId);
        wrapper.orderByDesc(com.smarten.module.datalake.entity.EnergyDataGap::getGapStart);

        Page<com.smarten.module.datalake.entity.EnergyDataGap> page =
                gapMapper.selectPage(query.toPage(), wrapper);
        Map<Long, EnergyMeter> meters = meters(page.getRecords().stream()
                .map(com.smarten.module.datalake.entity.EnergyDataGap::getMeterId).toList());
        Map<Long, String> devices = deviceNames(page.getRecords().stream()
                .map(gap -> meters.get(gap.getMeterId()) == null ? null : meters.get(gap.getMeterId()).getDeviceId()).toList());

        List<com.smarten.module.datalake.vo.GapVO> records = page.getRecords().stream().map(gap -> {
            com.smarten.module.datalake.vo.GapVO vo = new com.smarten.module.datalake.vo.GapVO();
            vo.setId(gap.getId());
            vo.setMeterId(gap.getMeterId());
            EnergyMeter meter = meters.get(gap.getMeterId());
            vo.setMeterName(meter == null ? null : meter.getMeterName());
            vo.setDeviceName(meter == null || meter.getDeviceId() == null ? null : devices.get(meter.getDeviceId()));
            vo.setGapStart(gap.getGapStart());
            vo.setGapEnd(gap.getGapEnd());
            vo.setExpectedCount(gap.getExpectedCount());
            vo.setActualCount(gap.getActualCount());
            vo.setMissingCount(gap.getMissingCount());
            vo.setStatus(gap.getStatus());
            vo.setFillTime(gap.getFillTime());
            return vo;
        }).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
    private <T> void applyEnterprise(LambdaQueryWrapper<T> wrapper, Long requested,
                                     com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> column) {
        if (dataScope.isPlatformAdmin()) {
            if (requested != null) {
                wrapper.eq(column, requested);
            }
        } else {
            wrapper.eq(column, dataScope.requireEnterpriseId());
        }
    }

    private Map<Long, EnergyMeter> meters(Collection<Long> ids) {
        List<Long> filtered = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (filtered.isEmpty()) {
            return Collections.emptyMap();
        }
        return meterMapper.selectBatchIds(filtered).stream()
                .collect(Collectors.toMap(EnergyMeter::getId, m -> m, (a, b) -> a));
    }

    private Map<Long, String> deviceNames(Collection<Long> ids) {
        List<Long> filtered = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (filtered.isEmpty()) {
            return Collections.emptyMap();
        }
        return deviceMapper.selectBatchIds(filtered).stream()
                .collect(Collectors.toMap(EnergyDevice::getId, EnergyDevice::getName, (a, b) -> a));
    }
}
