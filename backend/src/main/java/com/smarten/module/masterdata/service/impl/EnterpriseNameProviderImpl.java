package com.smarten.module.masterdata.service.impl;

import com.smarten.common.spi.EnterpriseNameProvider;
import com.smarten.module.masterdata.mapper.EnergyEnterpriseMapper;
import com.smarten.module.masterdata.entity.EnergyEnterprise;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link EnterpriseNameProvider} 的 masterdata 实现，供 system 模块查询企业名称。
 */
@Component
@RequiredArgsConstructor
public class EnterpriseNameProviderImpl implements EnterpriseNameProvider {

    private final EnergyEnterpriseMapper enterpriseMapper;

    @Override
    public Map<Long, String> findNames(Collection<Long> enterpriseIds) {
        if (enterpriseIds == null || enterpriseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return enterpriseMapper.selectBatchIds(enterpriseIds).stream()
                .collect(Collectors.toMap(EnergyEnterprise::getId, EnergyEnterprise::getName, (a, b) -> a));
    }
}
