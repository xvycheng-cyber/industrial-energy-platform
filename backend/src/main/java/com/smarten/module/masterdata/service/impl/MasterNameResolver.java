package com.smarten.module.masterdata.service.impl;

import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyEnterprise;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.entity.EnergyType;
import com.smarten.module.masterdata.entity.EnergyWorkshop;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyEnterpriseMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import com.smarten.module.masterdata.mapper.EnergyTypeMapper;
import com.smarten.module.masterdata.mapper.EnergyWorkshopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 主数据名称批量解析，避免列表页 N+1 查询。
 */
@Component
@RequiredArgsConstructor
public class MasterNameResolver {

    private final EnergyEnterpriseMapper enterpriseMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyMeterMapper meterMapper;
    private final EnergyTypeMapper energyTypeMapper;

    public Map<Long, String> enterpriseNames(Collection<Long> ids) {
        if (isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return enterpriseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EnergyEnterprise::getId, EnergyEnterprise::getName, (a, b) -> a));
    }

    public Map<Long, String> workshopNames(Collection<Long> ids) {
        if (isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return workshopMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EnergyWorkshop::getId, EnergyWorkshop::getName, (a, b) -> a));
    }

    public Map<Long, String> lineNames(Collection<Long> ids) {
        if (isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return lineMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EnergyLine::getId, EnergyLine::getName, (a, b) -> a));
    }

    public Map<Long, String> deviceNames(Collection<Long> ids) {
        if (isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return deviceMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EnergyDevice::getId, EnergyDevice::getName, (a, b) -> a));
    }

    public Map<Long, String> meterCodes(Collection<Long> ids) {
        if (isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return meterMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EnergyMeter::getId, EnergyMeter::getMeterCode, (a, b) -> a));
    }

    public Map<Long, String> energyTypeNames(Collection<Long> ids) {
        if (isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return energyTypeMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EnergyType::getId, EnergyType::getName, (a, b) -> a));
    }

    /** 提取非空 ID 并去重 */
    public static <T> List<Long> ids(Collection<T> source, Function<T, Long> getter) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(getter).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private boolean isEmpty(Collection<Long> ids) {
        return ids == null || ids.isEmpty();
    }
}
