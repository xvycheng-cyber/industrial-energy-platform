package com.smarten.module.diagnosis.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarten.module.baseline.entity.EnergyBaseline;
import com.smarten.module.baseline.mapper.EnergyBaselineMapper;
import com.smarten.module.diagnosis.dto.DiagnosisScanRequest;
import com.smarten.module.diagnosis.service.DiagnosisService;
import com.smarten.module.diagnosis.vo.DiagnosisScanResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时诊断：每小时扫描最近 2 小时；与手动扫描共用同一把锁，避免并发重复诊断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosisScheduler {

    private final EnergyBaselineMapper baselineMapper;
    private final DiagnosisService diagnosisService;

    @Scheduled(cron = "0 5 * * * *")
    public void hourlyScan() {
        List<Long> enterpriseIds = baselineMapper.selectList(new LambdaQueryWrapper<EnergyBaseline>()
                        .eq(EnergyBaseline::getStatus, 1)
                        .eq(EnergyBaseline::getDimType, 4))
                .stream().map(EnergyBaseline::getEnterpriseId).distinct().toList();
        for (Long enterpriseId : enterpriseIds) {
            try {
                DiagnosisScanRequest request = new DiagnosisScanRequest();
                request.setEnterpriseId(enterpriseId);
                LocalDateTime end = LocalDateTime.now();
                request.setStart(end.minusHours(2));
                request.setEnd(end);
                DiagnosisScanResultVO result = diagnosisService.scan(request);
                if (!result.isSkipped()) {
                    log.info("定时诊断完成 enterprise={} batch={} new={} merged={} reports={}",
                            enterpriseId, result.getBatchNo(), result.getNewAnomalies(),
                            result.getMergedAnomalies(), result.getReportCount());
                }
            } catch (Exception exception) {
                log.warn("定时诊断失败 enterprise={}: {}", enterpriseId, exception.getMessage());
            }
        }
    }
}
