package com.smarten.module.datalake.service;

import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.datalake.dto.CompletenessQueryDTO;
import com.smarten.module.datalake.mapper.EnergyRawDataMapper;
import com.smarten.module.datalake.vo.CompletenessVO;
import com.smarten.module.datalake.vo.DeviceCompletenessVO;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据完整率：应采点数（按仪表采集间隔推算）、有效入库数、缺失数。
 */
@Service
@RequiredArgsConstructor
public class DatalakeCompletenessService {

    private final EnergyRawDataMapper rawMapper;
    private final DataScopeHelper dataScope;

    public CompletenessVO compute(CompletenessQueryDTO query) {
        LocalDate date = query.getDate() == null ? LocalDate.now() : query.getDate();
        Long enterpriseId = dataScope.resolveEnterpriseId(query.getEnterpriseId());

        List<Map<String, Object>> expectedRows = rawMapper.countExpectedByMeter(enterpriseId, date);
        List<Map<String, Object>> validRows = rawMapper.countValidByMeter(enterpriseId, date);

        Map<Long, Integer> validMap = new HashMap<>();
        for (Map<String, Object> row : validRows) {
            validMap.put(((Number) row.get("meter_id")).longValue(), ((Number) row.get("valid_count")).intValue());
        }

        CompletenessVO result = new CompletenessVO();
        int totalExpected = 0;
        int totalValid = 0;

        for (Map<String, Object> row : expectedRows) {
            Long meterId = ((Number) row.get("meter_id")).longValue();
            Long deviceId = row.get("device_id") == null ? null : ((Number) row.get("device_id")).longValue();
            if (query.getMeterId() != null && !query.getMeterId().equals(meterId)) {
                continue;
            }
            if (query.getDeviceId() != null && !query.getDeviceId().equals(deviceId)) {
                continue;
            }
            int expected = ((Number) row.get("expected_count")).intValue();
            int valid = validMap.getOrDefault(meterId, 0);
            int effectiveValid = Math.min(valid, expected);

            DeviceCompletenessVO device = new DeviceCompletenessVO();
            device.setMeterId(meterId);
            device.setMeterCode((String) row.get("meter_code"));
            device.setMeterName((String) row.get("meter_name"));
            device.setDeviceId(deviceId);
            device.setDeviceName((String) row.get("device_name"));
            device.setExpectedCount(expected);
            device.setValidCount(valid);
            device.setMissingCount(Math.max(0, expected - valid));
            device.setCompletenessRate(rate(effectiveValid, expected));
            result.getDevices().add(device);

            totalExpected += expected;
            totalValid += effectiveValid;
        }

        result.setExpectedCount(totalExpected);
        result.setValidCount(totalValid);
        result.setMissingCount(Math.max(0, totalExpected - totalValid));
        result.setCompletenessRate(rate(totalValid, totalExpected));
        return result;
    }

    public BigDecimal todayRate(Long enterpriseId) {
        CompletenessQueryDTO query = new CompletenessQueryDTO();
        query.setEnterpriseId(enterpriseId);
        return compute(query).getCompletenessRate();
    }

    private BigDecimal rate(int valid, int expected) {
        if (expected <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(valid * 100.0 / expected).setScale(2, RoundingMode.HALF_UP);
    }
}
