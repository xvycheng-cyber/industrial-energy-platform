package com.smarten.module.saving.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.rectification.dto.TaskAuditRequest;
import com.smarten.module.rectification.entity.RectificationTask;
import com.smarten.module.rectification.enums.RectificationStatus;
import com.smarten.module.rectification.mapper.RectificationTaskMapper;
import com.smarten.module.rectification.service.RectificationService;
import com.smarten.module.saving.algorithm.SavingCalculator;
import com.smarten.module.saving.algorithm.SavingComputation;
import com.smarten.module.saving.algorithm.SavingConclusion;
import com.smarten.module.saving.algorithm.SavingOptions;
import com.smarten.module.saving.algorithm.SavingPoint;
import com.smarten.module.saving.dto.SavingCalculateRequest;
import com.smarten.module.saving.dto.SavingQuery;
import com.smarten.module.saving.entity.EnergySavingResult;
import com.smarten.module.saving.mapper.EnergySavingResultMapper;
import com.smarten.module.saving.vo.SavingResultVO;
import com.smarten.module.saving.vo.SavingSummaryVO;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 节能效果核算：按观察期逐小时“修正基线 − 实际”，支持复算、审核、排名、趋势与导出。
 */
@Service
@RequiredArgsConstructor
public class SavingService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EnergySavingResultMapper savingMapper;
    private final RectificationTaskMapper taskMapper;
    private final EnergyBaselineMapper baselineMapper;
    private final EnergyBaselineDetailMapper baselineDetailMapper;
    private final ProductionRecordMapper productionMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyLineMapper lineMapper;
    private final RectificationService rectificationService;
    private final DataScopeHelper dataScope;
    private final ObjectMapper objectMapper;

    private final SavingCalculator calculator = new SavingCalculator();

    /* ------------------------------- 核算 ------------------------------- */

    @Transactional(rollbackFor = Exception.class)
    public SavingResultVO calculate(Long taskId, SavingCalculateRequest request) {
        RectificationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        dataScope.checkAccessible(task.getEnterpriseId());
        if (RectificationStatus.of(task.getStatus()) != RectificationStatus.VERIFYING) {
            throw new BusinessException(ResultCode.TASK_STATUS_ILLEGAL, "只有效果验证中的任务可以核算");
        }

        SavingOptions defaults = SavingOptions.defaults();
        int days = request == null || request.getObservationDays() == null
                ? defaults.requiredDays() : request.getObservationDays();
        double price = request == null || request.getBasePrice() == null
                ? defaults.basePrice() : request.getBasePrice().doubleValue();
        SavingOptions options = new SavingOptions(days, defaults.minCompleteness(),
                defaults.outputTolerance(), defaults.minSavingRate(), price,
                defaults.peakMultiplier(), defaults.flatMultiplier(), defaults.valleyMultiplier());

        LocalDateTime start = request == null ? null : request.getStartTime();
        if (start == null) {
            start = task.getFinishTime() != null ? task.getFinishTime()
                    : task.getAuditTime() != null ? task.getAuditTime() : LocalDateTime.now().minusDays(days);
        }
        LocalDateTime end = start.plusDays(days);
        if (end.isAfter(LocalDateTime.now())) {
            end = LocalDateTime.now();
        }

        EnergyBaseline baseline = basisOf(task.getDeviceId());
        if (baseline == null) {
            throw new BusinessException(ResultCode.BASELINE_NOT_FOUND, "设备没有生效基线，无法核算");
        }
        List<EnergyBaselineDetail> details = baselineDetailMapper.selectList(
                new LambdaQueryWrapper<EnergyBaselineDetail>()
                        .eq(EnergyBaselineDetail::getBaselineId, baseline.getId())
                        .ge(EnergyBaselineDetail::getPeriodStart, start)
                        .lt(EnergyBaselineDetail::getPeriodStart, end)
                        .orderByAsc(EnergyBaselineDetail::getPeriodStart));
        Map<LocalDateTime, ProductionRecord> production = productionMap(task.getDeviceId(), start, end);

        List<SavingPoint> points = new ArrayList<>();
        for (EnergyBaselineDetail detail : details) {
            LocalDateTime hour = detail.getPeriodStart().truncatedTo(ChronoUnit.HOURS);
            ProductionRecord record = production.get(hour);
            double output = record == null || record.getOutputQty() == null ? 0 : record.getOutputQty().doubleValue();
            int runMinutes = record == null || record.getRunMinutes() == null ? 60 : record.getRunMinutes();
            int status = record == null || record.getStatus() == null ? 1 : record.getStatus();
            points.add(new SavingPoint(hour,
                    detail.getBaselineValue() == null ? 0 : detail.getBaselineValue().doubleValue(),
                    detail.getActualValue() == null ? 0 : detail.getActualValue().doubleValue(),
                    output, runMinutes, status));
        }

        double outputBefore = averageOutput(task.getDeviceId(), start.minusDays(7), start);
        SavingComputation computation = calculator.compute(points, start, end, outputBefore, options);

        Long previousCount = savingMapper.selectCount(new LambdaQueryWrapper<EnergySavingResult>()
                .eq(EnergySavingResult::getTaskId, taskId));
        List<EnergySavingResult> old = savingMapper.selectList(new LambdaQueryWrapper<EnergySavingResult>()
                .eq(EnergySavingResult::getTaskId, taskId)
                .eq(EnergySavingResult::getArchived, 0));
        for (EnergySavingResult item : old) {
            item.setArchived(1);
            savingMapper.updateById(item);
        }

        EnergySavingResult entity = new EnergySavingResult();
        entity.setTaskId(taskId);
        entity.setReportId(task.getReportId());
        entity.setEnterpriseId(task.getEnterpriseId());
        entity.setDeviceId(task.getDeviceId());
        entity.setVersion(previousCount.intValue() + 1);
        entity.setVerifyStart(start);
        entity.setVerifyEnd(end);
        entity.setSampleDays(computation.actualDays());
        entity.setActualDays(computation.actualDays());
        entity.setMethod(1);
        entity.setBaselineModelVersion(baseline.getModelVersion());
        entity.setBaselineTotal(decimal(computation.baselineTotal()));
        entity.setActualTotal(decimal(computation.actualTotal()));
        entity.setSavingValue(decimal(computation.saving()));
        entity.setSavingRate(decimal(computation.savingRate()));
        entity.setSavingAmount(decimal(computation.savingAmount()).setScale(2, RoundingMode.HALF_UP));
        entity.setUnitPrice(decimal(price));
        entity.setCompleteness(decimal(computation.completeness()));
        entity.setComparable(computation.comparable() ? 1 : 0);
        entity.setOutputBefore(decimal(computation.outputBefore()));
        entity.setOutputAfter(decimal(computation.outputAfter()));
        entity.setOutputChangeRate(decimal(computation.outputChangeRate()));
        entity.setConclusion(computation.conclusion().code() + ":" + computation.conclusion().label());
        entity.setConfidence(computation.completeness() >= 95 ? 1 : computation.completeness() >= 85 ? 2 : 3);
        entity.setDetailJson(writePoints(computation.points(), options));
        entity.setStatus(0);
        entity.setArchived(0);
        savingMapper.insert(entity);
        return detail(entity.getId());
    }

    /* ------------------------------- 查询 ------------------------------- */

    public PageResult<SavingResultVO> page(SavingQuery query) {
        LambdaQueryWrapper<EnergySavingResult> wrapper = new LambdaQueryWrapper<EnergySavingResult>()
                .orderByDesc(EnergySavingResult::getCreateTime);
        if (!Boolean.TRUE.equals(query.getIncludeArchived())) {
            wrapper.eq(EnergySavingResult::getArchived, 0);
        }
        if (query.getTaskId() != null) {
            wrapper.eq(EnergySavingResult::getTaskId, query.getTaskId());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(EnergySavingResult::getDeviceId, query.getDeviceId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergySavingResult::getStatus, query.getStatus());
        }
        if (query.getConclusionCode() != null) {
            wrapper.likeRight(EnergySavingResult::getConclusion, query.getConclusionCode() + ":");
        }
        if (query.getStartDate() != null) {
            wrapper.ge(EnergySavingResult::getVerifyStart, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.lt(EnergySavingResult::getVerifyStart, query.getEndDate().plusDays(1).atStartOfDay());
        }
        dataScope.applyScope(wrapper, EnergySavingResult::getEnterpriseId);
        Page<EnergySavingResult> page = savingMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<SavingResultVO> records = page.getRecords().stream()
                .map(item -> toVO(item, false)).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public SavingResultVO detail(Long id) {
        EnergySavingResult entity = savingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "核算结果不存在");
        }
        dataScope.checkAccessible(entity.getEnterpriseId());
        return toVO(entity, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, boolean pass, String remark) {
        EnergySavingResult entity = savingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "核算结果不存在");
        }
        dataScope.checkAccessible(entity.getEnterpriseId());
        if (entity.getStatus() != null && entity.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该核算结果已审核");
        }
        entity.setStatus(pass ? 1 : 2);
        entity.setAuditedBy(SecurityUtils.getUserIdOrNull());
        entity.setAuditedAt(LocalDateTime.now());
        entity.setAuditRemark(remark);
        savingMapper.updateById(entity);

        TaskAuditRequest audit = new TaskAuditRequest();
        audit.setPass(pass);
        audit.setReason(remark);
        audit.setActualSaving(entity.getSavingValue());
        audit.setRemark("节能核算结论：" + (pass ? "通过" : "驳回") + (remark == null ? "" : "，" + remark));
        rectificationService.verify(entity.getTaskId(), audit);
    }

    public SavingSummaryVO summary() {
        LambdaQueryWrapper<EnergySavingResult> wrapper = new LambdaQueryWrapper<EnergySavingResult>()
                .eq(EnergySavingResult::getArchived, 0);
        dataScope.applyScope(wrapper, EnergySavingResult::getEnterpriseId);
        List<EnergySavingResult> list = savingMapper.selectList(wrapper);

        SavingSummaryVO vo = new SavingSummaryVO();
        vo.setResultCount(list.size());
        Map<Long, EnergySavingResult> byDevice = new LinkedHashMap<>();
        Map<String, SavingSummaryVO.TrendItem> trend = new LinkedHashMap<>();
        for (EnergySavingResult item : list) {
            vo.setTotalSaving(vo.getTotalSaving().add(nz(item.getSavingValue())));
            vo.setTotalAmount(vo.getTotalAmount().add(nz(item.getSavingAmount())));
            EnergySavingResult current = byDevice.get(item.getDeviceId());
            if (current == null || nz(item.getSavingValue()).compareTo(nz(current.getSavingValue())) > 0) {
                byDevice.put(item.getDeviceId(), item);
            }
            String month = item.getVerifyStart() == null ? "" : item.getVerifyStart().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            SavingSummaryVO.TrendItem point = trend.computeIfAbsent(month, key -> {
                SavingSummaryVO.TrendItem created = new SavingSummaryVO.TrendItem();
                created.setMonth(key);
                created.setSaving(BigDecimal.ZERO);
                created.setAmount(BigDecimal.ZERO);
                return created;
            });
            point.setSaving(point.getSaving().add(nz(item.getSavingValue())));
            point.setAmount(point.getAmount().add(nz(item.getSavingAmount())));
        }

        Map<Long, String> names = deviceNames(byDevice.keySet());
        byDevice.entrySet().stream()
                .sorted((a, b) -> nz(b.getValue().getSavingValue()).compareTo(nz(a.getValue().getSavingValue())))
                .limit(10)
                .forEach(entry -> {
                    SavingSummaryVO.RankItem rank = new SavingSummaryVO.RankItem();
                    rank.setDeviceId(entry.getKey());
                    rank.setDeviceName(names.getOrDefault(entry.getKey(), "设备" + entry.getKey()));
                    rank.setSaving(nz(entry.getValue().getSavingValue()));
                    rank.setRate(nz(entry.getValue().getSavingRate()));
                    vo.getRanking().add(rank);
                });
        vo.getTrend().addAll(trend.values());
        return vo;
    }

    /* ------------------------------- 导出 ------------------------------- */

    public byte[] exportCsv(SavingQuery query) {
        query.setPageNum(1);
        query.setPageSize(500);
        List<SavingResultVO> rows = page(query).getRecords();
        StringBuilder builder = new StringBuilder();
        builder.append("任务号,设备,观察开始,观察结束,基线版本,基线能耗,实际能耗,节能量,节能率(%),节能金额(元),结论,状态,审核时间\n");
        for (SavingResultVO row : rows) {
            builder.append(csv(row.getTaskNo())).append(',')
                    .append(csv(row.getDeviceName())).append(',')
                    .append(row.getVerifyStart() == null ? "" : row.getVerifyStart().format(TIME)).append(',')
                    .append(row.getVerifyEnd() == null ? "" : row.getVerifyEnd().format(TIME)).append(',')
                    .append(csv(row.getBaselineModelVersion())).append(',')
                    .append(row.getBaselineTotal()).append(',')
                    .append(row.getActualTotal()).append(',')
                    .append(row.getSavingValue()).append(',')
                    .append(row.getSavingRate()).append(',')
                    .append(row.getSavingAmount()).append(',')
                    .append(csv(row.getConclusion())).append(',')
                    .append(csv(row.getStatusLabel())).append(',')
                    .append(row.getAuditedAt() == null ? "" : row.getAuditedAt().format(TIME)).append('\n');
        }
        byte[] content = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    /* ------------------------------- 辅助 ------------------------------- */

    private EnergyBaseline basisOf(Long deviceId) {
        return baselineMapper.selectOne(new LambdaQueryWrapper<EnergyBaseline>()
                .eq(EnergyBaseline::getDimType, 4)
                .eq(EnergyBaseline::getDimId, deviceId)
                .eq(EnergyBaseline::getStatus, 1)
                .orderByDesc(EnergyBaseline::getId)
                .last("LIMIT 1"));
    }

    private Map<LocalDateTime, ProductionRecord> productionMap(Long deviceId, LocalDateTime start, LocalDateTime end) {
        List<ProductionRecord> records = productionMapper.selectList(new LambdaQueryWrapper<ProductionRecord>()
                .eq(ProductionRecord::getDeviceId, deviceId)
                .ge(ProductionRecord::getStartTime, start)
                .lt(ProductionRecord::getStartTime, end));
        Map<LocalDateTime, ProductionRecord> map = new HashMap<>();
        for (ProductionRecord record : records) {
            map.put(record.getStartTime().truncatedTo(ChronoUnit.HOURS), record);
        }
        return map;
    }

    private double averageOutput(Long deviceId, LocalDateTime start, LocalDateTime end) {
        List<ProductionRecord> records = productionMapper.selectList(new LambdaQueryWrapper<ProductionRecord>()
                .eq(ProductionRecord::getDeviceId, deviceId)
                .ge(ProductionRecord::getStartTime, start)
                .lt(ProductionRecord::getStartTime, end)
                .gt(ProductionRecord::getRunMinutes, 0));
        return records.stream()
                .filter(item -> item.getOutputQty() != null && item.getOutputQty().signum() > 0)
                .mapToDouble(item -> item.getOutputQty().doubleValue())
                .average().orElse(0);
    }

    private SavingResultVO toVO(EnergySavingResult entity, boolean withDetail) {
        SavingResultVO vo = new SavingResultVO();
        vo.setId(entity.getId());
        vo.setTaskId(entity.getTaskId());
        vo.setDeviceId(entity.getDeviceId());
        vo.setVersion(entity.getVersion());
        vo.setVerifyStart(entity.getVerifyStart());
        vo.setVerifyEnd(entity.getVerifyEnd());
        vo.setActualDays(entity.getActualDays());
        vo.setSampleDays(entity.getSampleDays());
        vo.setBaselineModelVersion(entity.getBaselineModelVersion());
        vo.setBaselineTotal(entity.getBaselineTotal());
        vo.setActualTotal(entity.getActualTotal());
        vo.setSavingValue(entity.getSavingValue());
        vo.setSavingRate(entity.getSavingRate());
        vo.setSavingAmount(entity.getSavingAmount());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setCompleteness(entity.getCompleteness());
        vo.setComparable(entity.getComparable());
        vo.setOutputBefore(entity.getOutputBefore());
        vo.setOutputAfter(entity.getOutputAfter());
        vo.setOutputChangeRate(entity.getOutputChangeRate());
        vo.setConclusion(entity.getConclusion());
        vo.setConclusionCode(conclusionCode(entity.getConclusion()));
        vo.setReason(conclusionReason(entity.getConclusion()));
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(statusLabel(entity.getStatus()));
        vo.setArchived(entity.getArchived());
        vo.setAuditedAt(entity.getAuditedAt());
        vo.setAuditRemark(entity.getAuditRemark());
        vo.setCreateTime(entity.getCreateTime());

        RectificationTask task = taskMapper.selectById(entity.getTaskId());
        if (task != null) {
            vo.setTaskNo(task.getTaskNo());
            vo.setTitle(task.getTitle());
        }
        EnergyDevice device = deviceMapper.selectById(entity.getDeviceId());
        if (device != null) {
            vo.setDeviceName(device.getName());
            EnergyLine line = device.getLineId() == null ? null : lineMapper.selectById(device.getLineId());
            vo.setLineName(line == null ? null : line.getName());
        }
        if (withDetail) {
            vo.setPoints(readPoints(entity.getDetailJson()));
            vo.setVersions(versions(entity.getTaskId()));
        }
        return vo;
    }

    private List<SavingResultVO.VersionVO> versions(Long taskId) {
        return savingMapper.selectList(new LambdaQueryWrapper<EnergySavingResult>()
                        .eq(EnergySavingResult::getTaskId, taskId)
                        .orderByDesc(EnergySavingResult::getVersion))
                .stream().map(item -> {
                    SavingResultVO.VersionVO version = new SavingResultVO.VersionVO();
                    version.setId(item.getId());
                    version.setVersion(item.getVersion());
                    version.setConclusion(item.getConclusion());
                    version.setSavingValue(item.getSavingValue());
                    version.setCreateTime(item.getCreateTime());
                    version.setArchived(item.getArchived());
                    return version;
                }).toList();
    }

    private String writePoints(List<SavingPoint> points, SavingOptions options) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SavingPoint point : points) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("time", point.time().format(TIME));
            map.put("baseline", round(point.baseline()));
            map.put("actual", round(point.actual()));
            map.put("saving", round(point.baseline() - point.actual()));
            map.put("price", round(calculator.priceOf(point.time().getHour(), options)));
            list.add(map);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private List<SavingResultVO.PointVO> readPoints(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {
            });
            List<SavingResultVO.PointVO> points = new ArrayList<>();
            for (Map<String, Object> map : list) {
                SavingResultVO.PointVO point = new SavingResultVO.PointVO();
                point.setTime(LocalDateTime.parse(String.valueOf(map.get("time")).replace(' ', 'T')));
                point.setBaseline(new BigDecimal(String.valueOf(map.get("baseline"))));
                point.setActual(new BigDecimal(String.valueOf(map.get("actual"))));
                point.setSaving(new BigDecimal(String.valueOf(map.get("saving"))));
                point.setPrice(new BigDecimal(String.valueOf(map.get("price"))));
                points.add(point);
            }
            return points;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<Long, String> deviceNames(java.util.Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return deviceMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EnergyDevice::getId, EnergyDevice::getName, (a, b) -> a));
    }

    private int conclusionCode(String conclusion) {
        if (conclusion == null) {
            return 0;
        }
        return Integer.parseInt(conclusion.split(":")[0]);
    }

    private String conclusionReason(String conclusion) {
        if (conclusion == null || !conclusion.contains(":")) {
            return "";
        }
        return conclusion.substring(conclusion.indexOf(':') + 1);
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "待审核";
        }
        return switch (status) {
            case 1 -> "已生效";
            case 2 -> "已驳回";
            default -> "待审核";
        };
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\n") ? "\"" + escaped + "\"" : escaped;
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
