package com.smarten.module.diagnosis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.common.util.SecurityUtils;
import com.smarten.module.baseline.entity.EnergyBaseline;
import com.smarten.module.baseline.entity.EnergyBaselineDetail;
import com.smarten.module.baseline.mapper.EnergyBaselineDetailMapper;
import com.smarten.module.baseline.mapper.EnergyBaselineMapper;
import com.smarten.module.datalake.entity.ProductionRecord;
import com.smarten.module.datalake.mapper.ProductionRecordMapper;
import com.smarten.module.diagnosis.dto.AnomalyHandleRequest;
import com.smarten.module.diagnosis.dto.AnomalyQuery;
import com.smarten.module.diagnosis.dto.DiagnosisScanRequest;
import com.smarten.module.diagnosis.entity.DiagnosisReport;
import com.smarten.module.diagnosis.entity.DiagnosisRule;
import com.smarten.module.diagnosis.entity.EnergyAnomaly;
import com.smarten.module.diagnosis.engine.AnomalyScoring;
import com.smarten.module.diagnosis.engine.DetectionEngine;
import com.smarten.module.diagnosis.engine.DeviceSeries;
import com.smarten.module.diagnosis.engine.DiagnosisPoint;
import com.smarten.module.diagnosis.engine.RuleConfig;
import com.smarten.module.diagnosis.engine.Signal;
import com.smarten.module.diagnosis.mapper.DiagnosisReportMapper;
import com.smarten.module.diagnosis.mapper.DiagnosisRuleMapper;
import com.smarten.module.diagnosis.mapper.EnergyAnomalyMapper;
import com.smarten.module.diagnosis.vo.AnomalyVO;
import com.smarten.module.diagnosis.vo.DiagnosisOverviewVO;
import com.smarten.module.diagnosis.vo.DiagnosisReportVO;
import com.smarten.module.diagnosis.vo.DiagnosisScanResultVO;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyEnterprise;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.entity.EnergyWorkshop;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyEnterpriseMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.masterdata.mapper.EnergyWorkshopMapper;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 异常识别与节能诊断：扫描、合并、评分、报告与状态流转。
 */
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MERGE_GAP_HOURS = 6;

    private final EnergyAnomalyMapper anomalyMapper;
    private final DiagnosisReportMapper reportMapper;
    private final DiagnosisRuleMapper ruleMapper;
    private final DiagnosisRuleService ruleService;
    private final EnergyBaselineMapper baselineMapper;
    private final EnergyBaselineDetailMapper baselineDetailMapper;
    private final ProductionRecordMapper productionMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyEnterpriseMapper enterpriseMapper;
    private final DataScopeHelper dataScope;
    private final ObjectMapper objectMapper;

    private final DetectionEngine engine = new DetectionEngine();
    private final ReentrantLock scanLock = new ReentrantLock();

    /* ------------------------------- 扫描 ------------------------------- */

    public DiagnosisScanResultVO scan(DiagnosisScanRequest request) {
        long started = System.currentTimeMillis();
        if (!scanLock.tryLock()) {
            DiagnosisScanResultVO busy = new DiagnosisScanResultVO();
            busy.setSkipped(true);
            busy.setDurationMs(System.currentTimeMillis() - started);
            return busy;
        }
        try {
            Long enterpriseId = dataScope.resolveEnterpriseId(request.getEnterpriseId());
            if (enterpriseId == null) {
                enterpriseId = firstEnterpriseWithBaseline();
            }
            if (enterpriseId == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "没有可用的生效基线，请先训练基线模型");
            }
            dataScope.checkAccessible(enterpriseId);

            LocalDateTime end = request.getEnd() == null ? LocalDateTime.now() : request.getEnd();
            LocalDateTime start = request.getStart() == null ? end.minusHours(24) : request.getStart();

            List<RuleConfig> rules = ruleService.enabledRules();
            List<EnergyBaseline> baselines = activeBaselines(enterpriseId);
            List<DeviceSeries> series = buildSeries(baselines, start, end);
            List<Signal> signals = engine.detectAll(series, rules);

            Map<String, RuleConfig> ruleMap = rules.stream()
                    .collect(Collectors.toMap(RuleConfig::code, item -> item, (a, b) -> a));
            Map<String, DiagnosisRule> ruleEntities = ruleMapper.selectList(null).stream()
                    .collect(Collectors.toMap(DiagnosisRule::getRuleCode, item -> item, (a, b) -> a));
            Map<Long, EnergyDevice> devices = deviceMap(baselines);
            Map<Long, Integer> history = historyCounts(enterpriseId, end.minusDays(7));

            String batchNo = "SCAN-" + LocalDateTime.now().format(STAMP);
            int created = 0;
            int merged = 0;
            int reports = 0;

            for (Signal signal : signals) {
                RuleConfig rule = ruleMap.get(signal.ruleCode());
                if (rule == null) {
                    continue;
                }
                double peerDeviation = signal.evidence().get("peerDeviationRate") instanceof Number number
                        ? number.doubleValue() : 0;
                AnomalyScoring.Result score = AnomalyScoring.score(signal, rule,
                        history.getOrDefault(signal.deviceId(), 0), peerDeviation);

                EnergyAnomaly existing = findMergeTarget(signal);
                EnergyAnomaly anomaly;
                if (existing != null) {
                    extend(existing, signal, score);
                    anomalyMapper.updateById(existing);
                    anomaly = existing;
                    merged++;
                } else {
                    anomaly = createAnomaly(signal, score, devices.get(signal.deviceId()), batchNo);
                    anomalyMapper.insert(anomaly);
                    created++;
                }

                DiagnosisReport report = generateReport(anomaly, signal, rule, ruleEntities.get(rule.code()));
                if (report != null) {
                    anomaly.setReportId(report.getId());
                    anomalyMapper.updateById(anomaly);
                    reports++;
                }
            }

            DiagnosisScanResultVO result = new DiagnosisScanResultVO();
            result.setBatchNo(batchNo);
            result.setRuleCount(rules.size());
            result.setDeviceCount(series.size());
            result.setSignalCount(signals.size());
            result.setNewAnomalies(created);
            result.setMergedAnomalies(merged);
            result.setReportCount(reports);
            result.setDurationMs(System.currentTimeMillis() - started);
            return result;
        } finally {
            scanLock.unlock();
        }
    }

    private Long firstEnterpriseWithBaseline() {
        EnergyBaseline baseline = baselineMapper.selectOne(new LambdaQueryWrapper<EnergyBaseline>()
                .eq(EnergyBaseline::getStatus, 1)
                .orderByAsc(EnergyBaseline::getId)
                .last("LIMIT 1"));
        return baseline == null ? null : baseline.getEnterpriseId();
    }

    private List<EnergyBaseline> activeBaselines(Long enterpriseId) {
        List<EnergyBaseline> all = baselineMapper.selectList(new LambdaQueryWrapper<EnergyBaseline>()
                .eq(EnergyBaseline::getEnterpriseId, enterpriseId)
                .eq(EnergyBaseline::getDimType, 4)
                .eq(EnergyBaseline::getStatus, 1)
                .orderByAsc(EnergyBaseline::getId));
        Map<Long, EnergyBaseline> latest = new LinkedHashMap<>();
        all.forEach(item -> latest.put(item.getDimId(), item));
        return new ArrayList<>(latest.values());
    }

    private List<DeviceSeries> buildSeries(List<EnergyBaseline> baselines, LocalDateTime start, LocalDateTime end) {
        if (baselines.isEmpty()) {
            return List.of();
        }
        List<Long> baselineIds = baselines.stream().map(EnergyBaseline::getId).toList();
        List<EnergyBaselineDetail> details = baselineDetailMapper.selectList(
                new LambdaQueryWrapper<EnergyBaselineDetail>()
                        .in(EnergyBaselineDetail::getBaselineId, baselineIds)
                        .ge(EnergyBaselineDetail::getPeriodStart, start)
                        .lt(EnergyBaselineDetail::getPeriodStart, end)
                        .orderByAsc(EnergyBaselineDetail::getPeriodStart));
        Map<Long, List<EnergyBaselineDetail>> byBaseline = details.stream()
                .collect(Collectors.groupingBy(EnergyBaselineDetail::getBaselineId));

        List<Long> deviceIds = baselines.stream().map(EnergyBaseline::getDimId).toList();
        Map<Long, EnergyDevice> devices = deviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(EnergyDevice::getId, item -> item, (a, b) -> a));
        Map<String, ProductionRecord> production = new HashMap<>();
        List<ProductionRecord> records = productionMapper.selectList(new LambdaQueryWrapper<ProductionRecord>()
                .in(ProductionRecord::getDeviceId, deviceIds)
                .ge(ProductionRecord::getStartTime, start)
                .lt(ProductionRecord::getStartTime, end));
        for (ProductionRecord record : records) {
            production.put(record.getDeviceId() + "@" + record.getStartTime().truncatedTo(ChronoUnit.HOURS), record);
        }

        List<DeviceSeries> series = new ArrayList<>();
        for (EnergyBaseline baseline : baselines) {
            EnergyDevice device = devices.get(baseline.getDimId());
            if (device == null) {
                continue;
            }
            List<DiagnosisPoint> points = new ArrayList<>();
            for (EnergyBaselineDetail detail : byBaseline.getOrDefault(baseline.getId(), List.of())) {
                LocalDateTime hour = detail.getPeriodStart().truncatedTo(ChronoUnit.HOURS);
                double actual = value(detail.getActualValue());
                double base = value(detail.getBaselineValue());
                double deviation = detail.getDeviationRate() == null
                        ? (base == 0 ? 0 : (actual - base) / base * 100)
                        : detail.getDeviationRate().doubleValue();
                ProductionRecord record = production.get(device.getId() + "@" + hour);
                double load = record == null || record.getLoadRate() == null ? 100 : record.getLoadRate().doubleValue();
                double output = record == null || record.getOutputQty() == null ? 1 : record.getOutputQty().doubleValue();
                int runMinutes = record == null || record.getRunMinutes() == null ? 60 : record.getRunMinutes();
                int status = record == null || record.getStatus() == null ? 1 : record.getStatus();
                points.add(new DiagnosisPoint(hour, actual, base, deviation, load, output, runMinutes, status));
            }
            if (!points.isEmpty()) {
                series.add(new DeviceSeries(device.getId(), device.getDeviceType(), device.getLineId(), points));
            }
        }
        return series;
    }

    private EnergyAnomaly findMergeTarget(Signal signal) {
        return anomalyMapper.selectOne(new LambdaQueryWrapper<EnergyAnomaly>()
                .eq(EnergyAnomaly::getDeviceId, signal.deviceId())
                .eq(EnergyAnomaly::getRuleCode, signal.ruleCode())
                .in(EnergyAnomaly::getStatus, 0, 1)
                .ge(EnergyAnomaly::getPeriodEnd, signal.start().minusHours(MERGE_GAP_HOURS))
                .orderByDesc(EnergyAnomaly::getPeriodEnd)
                .last("LIMIT 1"));
    }

    private void extend(EnergyAnomaly anomaly, Signal signal, AnomalyScoring.Result score) {
        if (signal.end().isAfter(anomaly.getPeriodEnd())) {
            anomaly.setPeriodEnd(signal.end());
        }
        anomaly.setLastDetectTime(LocalDateTime.now());
        anomaly.setMergeCount((anomaly.getMergeCount() == null ? 1 : anomaly.getMergeCount()) + 1);
        anomaly.setDurationMinutes((int) Duration.between(anomaly.getPeriodStart(), anomaly.getPeriodEnd()).toMinutes());
        if (anomaly.getActualValue() == null || signal.actual() > anomaly.getActualValue().doubleValue()) {
            anomaly.setActualValue(decimal(signal.actual()));
        }
        if (anomaly.getDeviationRate() == null
                || Math.abs(signal.deviationRate()) > Math.abs(anomaly.getDeviationRate().doubleValue())) {
            anomaly.setDeviationRate(decimal(signal.deviationRate()));
        }
        if (anomaly.getRiskScore() == null || score.score() > anomaly.getRiskScore().doubleValue()) {
            anomaly.setRiskScore(BigDecimal.valueOf(score.score()));
        }
        anomaly.setSeverity(Math.max(anomaly.getSeverity() == null ? 1 : anomaly.getSeverity(), score.severity()));
    }

    private EnergyAnomaly createAnomaly(Signal signal, AnomalyScoring.Result score,
                                        EnergyDevice device, String batchNo) {
        EnergyAnomaly anomaly = new EnergyAnomaly();
        anomaly.setEnterpriseId(device == null ? null : device.getEnterpriseId());
        anomaly.setWorkshopId(device == null ? null : device.getWorkshopId());
        anomaly.setLineId(device == null ? null : device.getLineId());
        anomaly.setDeviceId(signal.deviceId());
        anomaly.setAnomalyType(signal.anomalyType());
        anomaly.setRuleCode(signal.ruleCode());
        anomaly.setSeverity(score.severity());
        anomaly.setRiskScore(BigDecimal.valueOf(score.score()));
        anomaly.setPeriodStart(signal.start());
        anomaly.setPeriodEnd(signal.end());
        anomaly.setDurationMinutes(signal.durationMinutes());
        anomaly.setMergeCount(1);
        anomaly.setActualValue(decimal(signal.actual()));
        anomaly.setBaselineValue(decimal(signal.baseline()));
        anomaly.setDeviationRate(decimal(signal.deviationRate()));
        anomaly.setThresholdValue(decimal(signal.threshold()));
        anomaly.setEvidenceJson(write(signal.evidence()));
        anomaly.setDetectTime(LocalDateTime.now());
        anomaly.setFirstDetectTime(LocalDateTime.now());
        anomaly.setLastDetectTime(LocalDateTime.now());
        anomaly.setDetectBatch(batchNo);
        anomaly.setStatus(0);
        return anomaly;
    }

    private DiagnosisReport generateReport(EnergyAnomaly anomaly, Signal signal, RuleConfig rule,
                                          DiagnosisRule entity) {
        DiagnosisReport existing = reportMapper.selectOne(new LambdaQueryWrapper<DiagnosisReport>()
                .eq(DiagnosisReport::getAnomalyId, anomaly.getId())
                .last("LIMIT 1"));
        EnergyDevice device = deviceMapper.selectById(anomaly.getDeviceId());
        String deviceName = device == null ? "设备" + anomaly.getDeviceId() : device.getName();

        List<String> causes = splitTemplate(entity == null ? null : entity.getCauseTemplate());
        List<String> suggestions = splitTemplate(entity == null ? null : entity.getSuggestionTemplate());
        if (causes.isEmpty()) {
            causes = List.of("工况与基线偏差较大", "设备效率下降", "运行参数不合理");
        }
        if (suggestions.isEmpty()) {
            suggestions = List.of("核查设备运行参数", "安排能效巡检", "复核计量与数据质量");
        }

        double savingHours = Math.max(0, anomaly.getDurationMinutes() == null ? 0 : anomaly.getDurationMinutes()) / 60.0;
        double saving = Math.max(0, signal.actual() - signal.baseline()) * Math.max(1, savingHours);
        java.time.format.DateTimeFormatter readable = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String description = String.format("%s在 %s 至 %s 命中规则 %s（%s），实际能耗 %.2f kWh，动态基线 %.2f kWh，偏差 %.2f%%，持续 %d 分钟。",
                deviceName, anomaly.getPeriodStart().format(readable), anomaly.getPeriodEnd().format(readable), rule.code(), rule.name(),
                signal.actual(), signal.baseline(), signal.deviationRate(), anomaly.getDurationMinutes());
        String conclusion = String.format("系统推断：设备处于%s状态，%s，建议优先排查：%s。",
                AnomalyScoring.levelOf(anomaly.getSeverity()), rule.name(), String.join("、", causes.stream().limit(2).toList()));

        DiagnosisReport report = existing == null ? new DiagnosisReport() : existing;
        if (existing == null) {
            report.setReportNo("DR" + LocalDateTime.now().format(STAMP) + "-" + anomaly.getId());
            report.setEnterpriseId(anomaly.getEnterpriseId());
            report.setAnomalyId(anomaly.getId());
            report.setDeviceId(anomaly.getDeviceId());
            report.setStatus(0);
        }
        report.setDescription(description);
        report.setConclusion(conclusion);
        report.setCauseCandidatesJson(write(causes));
        report.setSuggestionsJson(write(suggestions));
        report.setEstimatedSaving(decimal(saving));
        report.setRiskLevel(anomaly.getSeverity());
        report.setGeneratedAt(LocalDateTime.now());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleCode", rule.code());
        snapshot.put("ruleName", rule.name());
        snapshot.put("threshold", rule.threshold());
        snapshot.put("forced", rule.forced());
        report.setRuleSnapshotJson(write(snapshot));

        if (existing == null) {
            reportMapper.insert(report);
        } else {
            reportMapper.updateById(report);
        }
        return report;
    }

    /* ------------------------------- 查询 ------------------------------- */

    public PageResult<AnomalyVO> page(AnomalyQuery query) {
        LambdaQueryWrapper<EnergyAnomaly> wrapper = new LambdaQueryWrapper<EnergyAnomaly>()
                .orderByDesc(EnergyAnomaly::getDetectTime);
        if (query.getRuleCode() != null && !query.getRuleCode().isBlank()) {
            wrapper.eq(EnergyAnomaly::getRuleCode, query.getRuleCode());
        }
        if (query.getSeverity() != null) {
            wrapper.eq(EnergyAnomaly::getSeverity, query.getSeverity());
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergyAnomaly::getStatus, query.getStatus());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(EnergyAnomaly::getDeviceId, query.getDeviceId());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(EnergyAnomaly::getDetectTime, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.lt(EnergyAnomaly::getDetectTime, query.getEndDate().plusDays(1).atStartOfDay());
        }
        dataScope.applyScope(wrapper, EnergyAnomaly::getEnterpriseId);

        Page<EnergyAnomaly> page = anomalyMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<AnomalyVO> records = toVOs(page.getRecords());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public AnomalyVO detail(Long id) {
        EnergyAnomaly anomaly = anomalyMapper.selectById(id);
        if (anomaly == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "异常事件不存在");
        }
        dataScope.checkAccessible(anomaly.getEnterpriseId());
        List<AnomalyVO> list = toVOs(List.of(anomaly));
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, AnomalyHandleRequest request) {
        EnergyAnomaly anomaly = anomalyMapper.selectById(id);
        if (anomaly == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "异常事件不存在");
        }
        dataScope.checkAccessible(anomaly.getEnterpriseId());
        anomaly.setStatus(request.getStatus());
        anomaly.setRemark(request.getRemark());
        anomaly.setConfirmBy(SecurityUtils.getLoginUser() == null ? null : SecurityUtils.getLoginUser().userId());
        anomaly.setConfirmTime(LocalDateTime.now());
        anomalyMapper.updateById(anomaly);
    }

    public DiagnosisReportVO report(Long id) {
        DiagnosisReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "诊断报告不存在");
        }
        dataScope.checkAccessible(report.getEnterpriseId());
        return toReportVO(report);
    }

    public DiagnosisReportVO reportByAnomaly(Long anomalyId) {
        DiagnosisReport report = reportMapper.selectOne(new LambdaQueryWrapper<DiagnosisReport>()
                .eq(DiagnosisReport::getAnomalyId, anomalyId)
                .last("LIMIT 1"));
        if (report == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该异常尚未生成诊断报告");
        }
        dataScope.checkAccessible(report.getEnterpriseId());
        return toReportVO(report);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmCause(Long id, String confirmedCause) {
        DiagnosisReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "诊断报告不存在");
        }
        dataScope.checkAccessible(report.getEnterpriseId());
        report.setConfirmedCause(confirmedCause);
        report.setConfirmedBy(SecurityUtils.getLoginUser() == null ? null : SecurityUtils.getLoginUser().userId());
        report.setConfirmedTime(LocalDateTime.now());
        reportMapper.updateById(report);
    }

    public DiagnosisOverviewVO overview(Long enterpriseId, LocalDate start, LocalDate end) {
        Long scope = dataScope.resolveEnterpriseId(enterpriseId);
        LambdaQueryWrapper<EnergyAnomaly> wrapper = new LambdaQueryWrapper<>();
        if (scope != null) {
            wrapper.eq(EnergyAnomaly::getEnterpriseId, scope);
        } else {
            dataScope.applyScope(wrapper, EnergyAnomaly::getEnterpriseId);
        }
        if (start != null) {
            wrapper.ge(EnergyAnomaly::getDetectTime, start.atStartOfDay());
        }
        if (end != null) {
            wrapper.lt(EnergyAnomaly::getDetectTime, end.plusDays(1).atStartOfDay());
        }
        List<EnergyAnomaly> anomalies = anomalyMapper.selectList(wrapper);

        DiagnosisOverviewVO vo = new DiagnosisOverviewVO();
        vo.setTotal(anomalies.size());
        Map<Long, String> deviceNames = deviceNames(anomalies);
        Map<String, Long> trend = new LinkedHashMap<>();
        Map<String, Long> trendHigh = new LinkedHashMap<>();
        Map<Long, Long> deviceCount = anomalies.stream()
                .collect(Collectors.groupingBy(EnergyAnomaly::getDeviceId, Collectors.counting()));

        for (EnergyAnomaly anomaly : anomalies) {
            vo.getByRule().merge(anomaly.getRuleCode(), 1L, Long::sum);
            vo.getBySeverity().merge(AnomalyScoring.levelOf(anomaly.getSeverity()), 1L, Long::sum);
            vo.getByStatus().merge(statusLabel(anomaly.getStatus()), 1L, Long::sum);
            String day = anomaly.getDetectTime().toLocalDate().toString();
            trend.merge(day, 1L, Long::sum);
            if (anomaly.getSeverity() != null && anomaly.getSeverity() >= 3) {
                trendHigh.merge(day, 1L, Long::sum);
            }
            if (anomaly.getStatus() == 0) {
                vo.setOpenCount(vo.getOpenCount() + 1);
            } else if (anomaly.getStatus() == 2) {
                vo.setFalsePositiveCount(vo.getFalsePositiveCount() + 1);
            } else {
                vo.setHandledCount(vo.getHandledCount() + 1);
            }
            if (anomaly.getSeverity() != null && anomaly.getSeverity() >= 3) {
                vo.setHighRiskCount(vo.getHighRiskCount() + 1);
            }
        }
        trend.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> vo.getTrend().add(new DiagnosisOverviewVO.TrendPoint(
                        entry.getKey(), entry.getValue(), trendHigh.getOrDefault(entry.getKey(), 0L))));
        deviceCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> vo.getTopDevices().add(new DiagnosisOverviewVO.DeviceCount(
                        entry.getKey(), deviceNames.getOrDefault(entry.getKey(), "设备" + entry.getKey()), entry.getValue())));
        return vo;
    }

    /* ------------------------------- 辅助 ------------------------------- */

    private List<AnomalyVO> toVOs(List<EnergyAnomaly> anomalies) {
        if (anomalies.isEmpty()) {
            return List.of();
        }
        Set<Long> deviceIds = anomalies.stream().map(EnergyAnomaly::getDeviceId).collect(Collectors.toSet());
        Map<Long, EnergyDevice> devices = deviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(EnergyDevice::getId, item -> item, (a, b) -> a));
        Map<Long, String> lineNames = lineNames(devices.values().stream().map(EnergyDevice::getLineId).collect(Collectors.toSet()));
        Map<Long, String> workshopNames = workshopNames(devices.values().stream()
                .map(EnergyDevice::getWorkshopId).collect(Collectors.toSet()));
        Map<String, String> ruleNames = ruleMapper.selectList(null).stream()
                .collect(Collectors.toMap(DiagnosisRule::getRuleCode, DiagnosisRule::getRuleName, (a, b) -> a));
        Set<Long> reportIds = anomalies.stream().map(EnergyAnomaly::getReportId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> reportNos = reportIds.isEmpty() ? Map.of()
                : reportMapper.selectBatchIds(reportIds).stream()
                .collect(Collectors.toMap(DiagnosisReport::getId, DiagnosisReport::getReportNo, (a, b) -> a));

        List<AnomalyVO> result = new ArrayList<>();
        for (EnergyAnomaly anomaly : anomalies) {
            EnergyDevice device = devices.get(anomaly.getDeviceId());
            AnomalyVO vo = new AnomalyVO();
            vo.setId(anomaly.getId());
            vo.setDeviceId(anomaly.getDeviceId());
            vo.setDeviceName(device == null ? null : device.getName());
            vo.setDeviceCode(device == null ? null : device.getCode());
            vo.setDeviceType(device == null ? null : device.getDeviceType());
            vo.setLineName(device == null ? null : lineNames.get(device.getLineId()));
            vo.setWorkshopName(device == null ? null : workshopNames.get(device.getWorkshopId()));
            vo.setRuleCode(anomaly.getRuleCode());
            vo.setRuleName(ruleNames.getOrDefault(anomaly.getRuleCode(), anomaly.getRuleCode()));
            vo.setAnomalyType(anomaly.getAnomalyType());
            vo.setSeverity(anomaly.getSeverity());
            vo.setRiskLabel(AnomalyScoring.levelOf(anomaly.getSeverity() == null ? 1 : anomaly.getSeverity()));
            vo.setRiskScore(anomaly.getRiskScore());
            vo.setPeriodStart(anomaly.getPeriodStart());
            vo.setPeriodEnd(anomaly.getPeriodEnd());
            vo.setDurationMinutes(anomaly.getDurationMinutes());
            vo.setMergeCount(anomaly.getMergeCount());
            vo.setActualValue(anomaly.getActualValue());
            vo.setBaselineValue(anomaly.getBaselineValue());
            vo.setDeviationRate(anomaly.getDeviationRate());
            vo.setThresholdValue(anomaly.getThresholdValue());
            vo.setDetectTime(anomaly.getDetectTime());
            vo.setLastDetectTime(anomaly.getLastDetectTime());
            vo.setStatus(anomaly.getStatus());
            vo.setStatusLabel(statusLabel(anomaly.getStatus()));
            vo.setReportId(anomaly.getReportId());
            vo.setReportNo(anomaly.getReportId() == null ? null : reportNos.get(anomaly.getReportId()));
            vo.setRemark(anomaly.getRemark());
            vo.setEvidence(read(anomaly.getEvidenceJson()));
            result.add(vo);
        }
        return result;
    }

    private DiagnosisReportVO toReportVO(DiagnosisReport report) {
        EnergyAnomaly anomaly = anomalyMapper.selectById(report.getAnomalyId());
        EnergyDevice device = deviceMapper.selectById(report.getDeviceId());
        DiagnosisReportVO vo = new DiagnosisReportVO();
        vo.setId(report.getId());
        vo.setReportNo(report.getReportNo());
        vo.setAnomalyId(report.getAnomalyId());
        vo.setDeviceId(report.getDeviceId());
        vo.setDeviceName(device == null ? null : device.getName());
        vo.setDeviceCode(device == null ? null : device.getCode());
        if (anomaly != null) {
            vo.setRuleCode(anomaly.getRuleCode());
            vo.setRuleName(ruleMapper.selectList(null).stream()
                    .filter(item -> item.getRuleCode().equals(anomaly.getRuleCode()))
                    .map(DiagnosisRule::getRuleName).findFirst().orElse(anomaly.getRuleCode()));
            vo.setActualValue(anomaly.getActualValue());
            vo.setBaselineValue(anomaly.getBaselineValue());
            vo.setDeviationRate(anomaly.getDeviationRate());
        }
        vo.setDescription(report.getDescription());
        vo.setConclusion(report.getConclusion());
        vo.setCauses(readList(report.getCauseCandidatesJson()));
        vo.setSuggestions(readList(report.getSuggestionsJson()));
        vo.setConfirmedCause(report.getConfirmedCause());
        vo.setConfirmedTime(report.getConfirmedTime());
        vo.setEstimatedSaving(report.getEstimatedSaving());
        vo.setRiskLevel(report.getRiskLevel());
        vo.setRiskLabel(AnomalyScoring.levelOf(report.getRiskLevel() == null ? 1 : report.getRiskLevel()));
        vo.setStatus(report.getStatus());
        vo.setGeneratedAt(report.getGeneratedAt());
        vo.setRuleSnapshot(read(report.getRuleSnapshotJson()));
        return vo;
    }

    private Map<Long, String> lineNames(Set<Long> ids) {
        Set<Long> filtered = ids.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        return filtered.isEmpty() ? Map.of() : lineMapper.selectBatchIds(filtered).stream()
                .collect(Collectors.toMap(EnergyLine::getId, EnergyLine::getName, (a, b) -> a));
    }

    private Map<Long, String> workshopNames(Set<Long> ids) {
        Set<Long> filtered = ids.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        return filtered.isEmpty() ? Map.of() : workshopMapper.selectBatchIds(filtered).stream()
                .collect(Collectors.toMap(EnergyWorkshop::getId, EnergyWorkshop::getName, (a, b) -> a));
    }

    private Map<Long, String> deviceNames(List<EnergyAnomaly> anomalies) {
        Set<Long> ids = anomalies.stream().map(EnergyAnomaly::getDeviceId).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of() : deviceMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EnergyDevice::getId, EnergyDevice::getName, (a, b) -> a));
    }

    private Map<Long, EnergyDevice> deviceMap(List<EnergyBaseline> baselines) {
        if (baselines.isEmpty()) {
            return Map.of();
        }
        return deviceMapper.selectBatchIds(baselines.stream().map(EnergyBaseline::getDimId).toList()).stream()
                .collect(Collectors.toMap(EnergyDevice::getId, item -> item, (a, b) -> a));
    }

    private Map<Long, Integer> historyCounts(Long enterpriseId, LocalDateTime from) {
        QueryWrapper<EnergyAnomaly> wrapper = new QueryWrapper<>();
        wrapper.select("device_id", "COUNT(*) AS cnt")
                .eq("enterprise_id", enterpriseId)
                .ge("detect_time", from)
                .eq("deleted", 0)
                .groupBy("device_id");
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : anomalyMapper.selectMaps(wrapper)) {
            result.put(((Number) row.get("device_id")).longValue(), ((Number) row.get("cnt")).intValue());
        }
        return result;
    }

    public String statusLabel(Integer status) {
        if (status == null) {
            return "待确认";
        }
        return switch (status) {
            case 1 -> "已确认";
            case 2 -> "误报";
            case 3 -> "已转诊断";
            case 4 -> "已关闭";
            default -> "待确认";
        };
    }

    private List<String> splitTemplate(String template) {
        if (template == null || template.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(template.split("[;；]"))
                .map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<String, Object> read(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private double value(BigDecimal number) {
        return number == null ? 0 : number.doubleValue();
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}
