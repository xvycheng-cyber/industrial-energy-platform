package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarten.module.datalake.entity.DataIngestionLog;
import com.smarten.module.datalake.entity.EnergyAggregate;
import com.smarten.module.datalake.mapper.DataIngestionLogMapper;
import com.smarten.module.datalake.mapper.EnergyAggregateMapper;
import com.smarten.module.datalake.vo.DashboardVO;
import com.smarten.module.datalake.vo.DeviceRankVO;
import com.smarten.module.datalake.vo.EnergySliceVO;
import com.smarten.module.datalake.vo.ProfilePointVO;
import com.smarten.module.datalake.vo.ProfileVO;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyEnterprise;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.entity.EnergyType;
import com.smarten.module.masterdata.entity.EnergyWorkshop;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyEnterpriseMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.masterdata.mapper.EnergyTypeMapper;
import com.smarten.module.masterdata.mapper.EnergyWorkshopMapper;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ADS 应用层：驾驶舱与能耗画像统计。
 */
@Service
@RequiredArgsConstructor
public class DatalakeAdsService {

    private final EnergyAggregateMapper aggregateMapper;
    private final DataIngestionLogMapper logMapper;
    private final EnergyEnterpriseMapper enterpriseMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyTypeMapper energyTypeMapper;
    private final DatalakeCompletenessService completenessService;
    private final DataScopeHelper dataScope;

    public DashboardVO dashboard(LocalDate date, Long enterpriseId) {
        LocalDate day = date == null ? LocalDate.now() : date;
        Long scope = dataScope.resolveEnterpriseId(enterpriseId);
        LocalDateTime dayStart = day.atStartOfDay();

        List<EnergyAggregate> daily = aggregateMapper.selectList(new LambdaQueryWrapper<EnergyAggregate>()
                .eq(EnergyAggregate::getDimType, 1)
                .eq(EnergyAggregate::getPeriodType, 2)
                .eq(EnergyAggregate::getPeriodStart, dayStart)
                .eq(scope != null, EnergyAggregate::getEnterpriseId, scope));

        DashboardVO vo = new DashboardVO();
        vo.setDate(day);
        BigDecimal totalEnergy = daily.stream().map(EnergyAggregate::getEnergyValue)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = daily.stream().map(EnergyAggregate::getCostAmount)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalEnergy(totalEnergy.setScale(2, RoundingMode.HALF_UP));
        vo.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));

        Map<Long, EnergyType> types = energyTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(EnergyType::getId, t -> t, (a, b) -> a));
        Map<Long, BigDecimal> byType = new HashMap<>();
        for (EnergyAggregate item : daily) {
            byType.merge(item.getEnergyTypeId(), item.getEnergyValue() == null ? BigDecimal.ZERO : item.getEnergyValue(), BigDecimal::add);
        }
        byType.forEach((typeId, value) -> {
            EnergyType type = types.get(typeId);
            EnergySliceVO slice = new EnergySliceVO();
            slice.setEnergyTypeId(typeId);
            slice.setEnergyTypeName(type == null ? "未知" : type.getName());
            slice.setUnit(type == null ? null : type.getUnit());
            slice.setEnergyValue(value.setScale(2, RoundingMode.HALF_UP));
            vo.getEnergyTypes().add(slice);
        });

        List<EnergyAggregate> top = aggregateMapper.selectList(new LambdaQueryWrapper<EnergyAggregate>()
                .eq(EnergyAggregate::getDimType, 4)
                .eq(EnergyAggregate::getPeriodType, 2)
                .eq(EnergyAggregate::getPeriodStart, dayStart)
                .eq(scope != null, EnergyAggregate::getEnterpriseId, scope));
        Map<Long, BigDecimal> deviceEnergy = new HashMap<>();
        for (EnergyAggregate item : top) {
            deviceEnergy.merge(item.getDimId(), item.getEnergyValue() == null ? BigDecimal.ZERO : item.getEnergyValue(), BigDecimal::add);
        }
        List<Long> deviceIds = new ArrayList<>(deviceEnergy.keySet());
        Map<Long, String> deviceNames = deviceIds.isEmpty() ? Map.of()
                : deviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(EnergyDevice::getId, EnergyDevice::getName, (a, b) -> a));
        deviceEnergy.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    DeviceRankVO rank = new DeviceRankVO();
                    rank.setDeviceId(entry.getKey());
                    rank.setDeviceName(deviceNames.getOrDefault(entry.getKey(), "设备" + entry.getKey()));
                    rank.setEnergyValue(entry.getValue().setScale(2, RoundingMode.HALF_UP));
                    vo.getTopDevices().add(rank);
                });

        List<EnergyAggregate> hourly = aggregateMapper.selectList(new LambdaQueryWrapper<EnergyAggregate>()
                .eq(EnergyAggregate::getDimType, 1)
                .eq(EnergyAggregate::getPeriodType, 1)
                .ge(EnergyAggregate::getPeriodStart, dayStart)
                .lt(EnergyAggregate::getPeriodStart, dayStart.plusDays(1))
                .eq(scope != null, EnergyAggregate::getEnterpriseId, scope));
        Map<LocalDateTime, BigDecimal> hourlyMap = new HashMap<>();
        for (EnergyAggregate item : hourly) {
            hourlyMap.merge(item.getPeriodStart(), item.getEnergyValue() == null ? BigDecimal.ZERO : item.getEnergyValue(), BigDecimal::add);
        }
        hourlyMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            ProfilePointVO point = new ProfilePointVO();
            point.setTime(entry.getKey());
            point.setLabel(entry.getKey().getHour() + ":00");
            point.setEnergyValue(entry.getValue().setScale(2, RoundingMode.HALF_UP));
            vo.getHourly().add(point);
        });

        var completenessQuery = new com.smarten.module.datalake.dto.CompletenessQueryDTO();
        completenessQuery.setDate(day);
        completenessQuery.setEnterpriseId(scope);
        vo.setCompletenessRate(completenessService.compute(completenessQuery).getCompletenessRate());

        List<DataIngestionLog> logs = logMapper.selectList(new LambdaQueryWrapper<DataIngestionLog>()
                .ge(DataIngestionLog::getStartTime, dayStart)
                .lt(DataIngestionLog::getStartTime, dayStart.plusDays(1))
                .eq(scope != null, DataIngestionLog::getEnterpriseId, scope)
                .orderByDesc(DataIngestionLog::getStartTime));
        vo.setTodayBatchCount(logs.size());
        vo.setLastBatchNo(logs.isEmpty() ? null : logs.get(0).getBatchNo());
        vo.setDeviceCount(deviceMapper.selectCount(new LambdaQueryWrapper<EnergyDevice>()
                .eq(scope != null, EnergyDevice::getEnterpriseId, scope)).intValue());
        return vo;
    }

    public ProfileVO profile(Integer dimType, Long dimId, Integer days) {
        int range = days == null || days <= 0 ? 7 : Math.min(days, 90);
        int type = dimType == null ? 4 : dimType;
        Long id = dimId;
        if (id == null) {
            id = type == 1 ? dataScope.requireEnterpriseId() : null;
        }
        if (id == null) {
            throw new com.smarten.common.exception.BusinessException(
                    com.smarten.common.api.ResultCode.BAD_REQUEST, "缺少统计对象");
        }

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(range - 1);
        List<EnergyAggregate> rows = aggregateMapper.selectList(new LambdaQueryWrapper<EnergyAggregate>()
                .eq(EnergyAggregate::getDimType, type)
                .eq(EnergyAggregate::getDimId, id)
                .eq(EnergyAggregate::getPeriodType, 2)
                .ge(EnergyAggregate::getPeriodStart, start.atStartOfDay())
                .lt(EnergyAggregate::getPeriodStart, end.plusDays(1).atStartOfDay())
                .orderByAsc(EnergyAggregate::getPeriodStart));

        Map<LocalDate, BigDecimal> daily = new HashMap<>();
        Map<Long, BigDecimal> byType = new HashMap<>();
        for (EnergyAggregate item : rows) {
            LocalDate day = item.getPeriodStart().toLocalDate();
            daily.merge(day, item.getEnergyValue() == null ? BigDecimal.ZERO : item.getEnergyValue(), BigDecimal::add);
            byType.merge(item.getEnergyTypeId(), item.getEnergyValue() == null ? BigDecimal.ZERO : item.getEnergyValue(), BigDecimal::add);
        }

        ProfileVO vo = new ProfileVO();
        vo.setDimType(type);
        vo.setDimId(id);
        vo.setDimName(resolveName(type, id));
        vo.setDays(range);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = null;
        BigDecimal min = null;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            BigDecimal value = daily.getOrDefault(d, BigDecimal.ZERO);
            total = total.add(value);
            max = max == null || value.compareTo(max) > 0 ? value : max;
            min = min == null || value.compareTo(min) < 0 ? value : min;
            ProfilePointVO point = new ProfilePointVO();
            point.setTime(d.atStartOfDay());
            point.setLabel(d.toString());
            point.setEnergyValue(value.setScale(2, RoundingMode.HALF_UP));
            vo.getSeries().add(point);
        }
        vo.setTotalEnergy(total.setScale(2, RoundingMode.HALF_UP));
        vo.setAvgEnergy(total.divide(BigDecimal.valueOf(range), 2, RoundingMode.HALF_UP));
        vo.setMaxEnergy(max == null ? BigDecimal.ZERO : max);
        vo.setMinEnergy(min == null ? BigDecimal.ZERO : min);

        Map<Long, EnergyType> types = energyTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(EnergyType::getId, t -> t, (a, b) -> a));
        byType.forEach((typeId, value) -> {
            EnergyType energyType = types.get(typeId);
            EnergySliceVO slice = new EnergySliceVO();
            slice.setEnergyTypeId(typeId);
            slice.setEnergyTypeName(energyType == null ? "未知" : energyType.getName());
            slice.setUnit(energyType == null ? null : energyType.getUnit());
            slice.setEnergyValue(value.setScale(2, RoundingMode.HALF_UP));
            vo.getEnergyTypes().add(slice);
        });
        vo.getEnergyTypes().sort(Comparator.comparing(EnergySliceVO::getEnergyValue).reversed());
        return vo;
    }

    private String resolveName(int dimType, Long id) {
        return switch (dimType) {
            case 1 -> {
                EnergyEnterprise enterprise = enterpriseMapper.selectById(id);
                yield enterprise == null ? null : enterprise.getName();
            }
            case 2 -> {
                EnergyWorkshop workshop = workshopMapper.selectById(id);
                yield workshop == null ? null : workshop.getName();
            }
            case 3 -> {
                EnergyLine line = lineMapper.selectById(id);
                yield line == null ? null : line.getName();
            }
            default -> {
                EnergyDevice device = deviceMapper.selectById(id);
                yield device == null ? null : device.getName();
            }
        };
    }
}
