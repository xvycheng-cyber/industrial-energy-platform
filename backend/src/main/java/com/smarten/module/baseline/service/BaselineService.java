package com.smarten.module.baseline.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.baseline.algorithm.BaselineModel;
import com.smarten.module.baseline.algorithm.BaselineTrainer;
import com.smarten.module.baseline.algorithm.Prediction;
import com.smarten.module.baseline.algorithm.Sample;
import com.smarten.module.baseline.algorithm.TrainOptions;
import com.smarten.module.baseline.algorithm.TrainResult;
import com.smarten.module.baseline.dto.BaselineModelQuery;
import com.smarten.module.baseline.dto.BaselinePredictRequest;
import com.smarten.module.baseline.dto.BaselineRecordQuery;
import com.smarten.module.baseline.dto.BaselineTrainRequest;
import com.smarten.module.baseline.entity.EnergyBaseline;
import com.smarten.module.baseline.entity.EnergyBaselineDetail;
import com.smarten.module.baseline.mapper.EnergyBaselineDetailMapper;
import com.smarten.module.baseline.mapper.EnergyBaselineMapper;
import com.smarten.module.baseline.vo.BaselineErrorAnalysisVO;
import com.smarten.module.baseline.vo.BaselineModelVO;
import com.smarten.module.baseline.vo.BaselineRecordVO;
import com.smarten.module.baseline.vo.BaselineTrainResultVO;
import com.smarten.module.datalake.entity.EnergyDetail;
import com.smarten.module.datalake.entity.ProductionRecord;
import com.smarten.module.datalake.mapper.EnergyAggregateMapper;
import com.smarten.module.datalake.mapper.EnergyDetailMapper;
import com.smarten.module.datalake.mapper.ProductionRecordMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 动态能耗基线：训练、预测、模型版本管理与误差分析。
 */
@Service
@RequiredArgsConstructor
public class BaselineService {

    private static final int GRANULARITY_HOUR = 3;
    private static final int DETAIL_TYPE_REGRESSION = 1;
    private static final int DETAIL_TYPE_GROUP_MEAN = 2;
    private static final int DETAIL_TYPE_HISTORICAL = 4;
    private static final int DETAIL_TYPE_STANDBY = 5;

    private static final DateTimeFormatter VERSION_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EnergyBaselineMapper baselineMapper;
    private final EnergyBaselineDetailMapper detailMapper;
    private final ProductionRecordMapper productionMapper;
    private final EnergyDetailMapper energyDetailMapper;
    private final EnergyAggregateMapper aggregateMapper;
    private final EnergyEnterpriseMapper enterpriseMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyTypeMapper energyTypeMapper;
    private final DataScopeHelper dataScope;
    private final ObjectMapper objectMapper;

    private final BaselineTrainer trainer = new BaselineTrainer();

    /* ------------------------------- 训练 ------------------------------- */

    @Transactional(rollbackFor = Exception.class)
    public BaselineTrainResultVO train(BaselineTrainRequest request) {
        Scope scope = resolveScope(request.getDimType(), request.getDimId());
        List<Bucket> buckets = loadBuckets(scope, request.getEnergyTypeId(), request.getStartDate(), request.getEndDate());
        List<Sample> samples = new ArrayList<>();
        int qualityRejected = 0;
        for (Bucket bucket : buckets) {
            if (bucket.actual() == null) {
                continue;
            }
            if (!bucket.qualityOk()) {
                qualityRejected++;
                continue;
            }
            samples.add(bucket.toSample(scope.dimId()));
        }
        if (samples.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "训练区间内没有可用样本，请先补齐生产工况与能耗数据");
        }

        TrainResult result = trainer.train(samples, TrainOptions.defaults());
        BaselineModel model = result.model();

        EnergyBaseline entity = new EnergyBaseline();
        entity.setEnterpriseId(scope.enterpriseId());
        entity.setDimType(request.getDimType());
        entity.setDimId(request.getDimId());
        entity.setEnergyTypeId(request.getEnergyTypeId());
        entity.setGranularity(GRANULARITY_HOUR);
        entity.setBaselineType(toBaselineType(model.getAlgo()));
        entity.setModelVersion(nextVersion(scope, request.getEnergyTypeId()));
        entity.setAlgo(model.getAlgo());
        entity.setSampleCount(result.normalSamples());
        entity.setTrainStart(request.getStartDate());
        entity.setTrainEnd(request.getEndDate());
        entity.setR2(decimal(model.getR2()));
        entity.setMae(decimal(model.getMae()));
        entity.setRmse(decimal(model.getRmse()));
        entity.setMape(decimal(model.getMape()));
        entity.setFallbackCount(0);
        entity.setSampleRejected(result.rejected() + qualityRejected);
        entity.setOutlierCount(result.outliers());
        entity.setParamsJson(writeJson(model.toParams()));
        entity.setFeatureJson(writeJson(List.of(model.getFeatureNames())));
        entity.setConfidence(model.getAlgo().equals(BaselineModel.RIDGE)
                ? (result.normalSamples() >= 100 ? 1 : 2) : 3);
        entity.setStatus(0);
        entity.setCalcTime(LocalDateTime.now());

        boolean activate = request.getActivate() == null || request.getActivate();
        if (activate) {
            archiveActive(scope, request.getEnergyTypeId());
            entity.setStatus(1);
            entity.setEffectiveFrom(LocalDateTime.now());
        }
        baselineMapper.insert(entity);

        int predicted = predictRange(entity, model, scope, request.getStartDate(), request.getEndDate());

        BaselineTrainResultVO vo = new BaselineTrainResultVO();
        vo.setModelId(entity.getId());
        vo.setModelVersion(entity.getModelVersion());
        vo.setAlgo(model.getAlgo());
        vo.setSampleCount(entity.getSampleCount());
        vo.setNormalSamples(result.normalSamples());
        vo.setStandbySamples(result.standbySamples());
        vo.setRejectedSamples(result.rejected() + qualityRejected);
        vo.setOutlierSamples(result.outliers());
        vo.setRequiredSamples(result.requiredSamples());
        vo.setMae(decimal(model.getMae()));
        vo.setRmse(decimal(model.getRmse()));
        vo.setMape(decimal(model.getMape()));
        vo.setR2(decimal(model.getR2()));
        vo.setPredictedCount(predicted);
        vo.setActivated(activate);
        vo.setFallbacks(new ArrayList<>(model.getFallbackLog()));
        if (qualityRejected > 0) {
            vo.getFallbacks().add("质量校验剔除 " + qualityRejected + " 条（DWD 质量标记异常或补录估算）");
        }
        return vo;
    }

    /* ------------------------------- 预测 ------------------------------- */

    @Transactional(rollbackFor = Exception.class)
    public int predict(BaselinePredictRequest request) {
        Scope scope = resolveScope(request.getDimType(), request.getDimId());
        EnergyBaseline baseline = request.getBaselineId() != null
                ? baselineMapper.selectById(request.getBaselineId())
                : activeModel(scope, request.getEnergyTypeId());
        if (baseline == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "没有可用的基线模型，请先训练");
        }
        dataScope.checkAccessible(baseline.getEnterpriseId());
        BaselineModel model = readModel(baseline);
        return predictRange(baseline, model, scope, request.getStartDate(), request.getEndDate());
    }

    private int predictRange(EnergyBaseline entity, BaselineModel model, Scope scope, LocalDate start, LocalDate end) {
        List<Bucket> buckets = loadBuckets(scope, entity.getEnergyTypeId(), start, end);
        int fallback = 0;
        int count = 0;
        for (Bucket bucket : buckets) {
            Sample sample = bucket.toSample(scope.dimId());
            Prediction prediction = model.predictDetailed(sample);
            if (prediction.baselineType() == DETAIL_TYPE_GROUP_MEAN
                    || prediction.baselineType() == DETAIL_TYPE_HISTORICAL) {
                fallback++;
            }
            EnergyBaselineDetail detail = detailMapper.selectOne(new LambdaQueryWrapper<EnergyBaselineDetail>()
                    .eq(EnergyBaselineDetail::getBaselineId, entity.getId())
                    .eq(EnergyBaselineDetail::getPeriodStart, bucket.hour()));
            boolean create = detail == null;
            if (create) {
                detail = new EnergyBaselineDetail();
                detail.setBaselineId(entity.getId());
                detail.setEnterpriseId(entity.getEnterpriseId());
                detail.setDimType(entity.getDimType());
                detail.setDimId(entity.getDimId());
                detail.setEnergyTypeId(entity.getEnergyTypeId());
                detail.setPeriodStart(bucket.hour());
                detail.setPeriodEnd(bucket.hour().plusHours(1));
            }
            detail.setBaselineValue(decimal(prediction.value()));
            detail.setActualValue(bucket.actual() == null ? null : decimal(bucket.actual()));
            detail.setBaselineType(prediction.baselineType());
            detail.setModelVersion(entity.getModelVersion());
            detail.setConditionJson(writeJson(bucket.condition()));
            detail.setFallbackReason(prediction.fallbackReason());
            detail.setEstimated(bucket.actual() == null ? 1 : 0);
            detail.setComparable(bucket.actual() == null ? 0 : 1);
            detail.setComputedAt(LocalDateTime.now());
            if (bucket.actual() != null) {
                BigDecimal deviation = decimal(bucket.actual()).subtract(detail.getBaselineValue());
                detail.setDeviationValue(deviation.setScale(4, RoundingMode.HALF_UP));
                if (detail.getBaselineValue() != null && detail.getBaselineValue().signum() != 0) {
                    detail.setDeviationRate(deviation.multiply(BigDecimal.valueOf(100))
                            .divide(detail.getBaselineValue(), 4, RoundingMode.HALF_UP));
                }
            }
            if (create) {
                detailMapper.insert(detail);
            } else {
                detailMapper.updateById(detail);
            }
            count++;
        }
        entity.setFallbackCount((entity.getFallbackCount() == null ? 0 : entity.getFallbackCount()) + fallback);
        baselineMapper.updateById(entity);
        return count;
    }

    /* ----------------------------- 模型管理 ----------------------------- */

    public PageResult<BaselineModelVO> models(BaselineModelQuery query) {
        LambdaQueryWrapper<EnergyBaseline> wrapper = new LambdaQueryWrapper<EnergyBaseline>()
                .orderByDesc(EnergyBaseline::getId);
        if (query.getDimType() != null) {
            wrapper.eq(EnergyBaseline::getDimType, query.getDimType());
        }
        if (query.getDimId() != null) {
            wrapper.eq(EnergyBaseline::getDimId, query.getDimId());
        }
        if (query.getEnergyTypeId() != null) {
            wrapper.eq(EnergyBaseline::getEnergyTypeId, query.getEnergyTypeId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergyBaseline::getStatus, query.getStatus());
        }
        Page<EnergyBaseline> page = baselineMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Map<Long, String> dimNames = dimNames(page.getRecords());
        Map<Long, String> typeNames = energyTypeNames();
        List<BaselineModelVO> records = page.getRecords().stream()
                .map(item -> toModelVO(item, dimNames, typeNames))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public BaselineModelVO modelDetail(Long id) {
        EnergyBaseline entity = requireModel(id);
        return toModelVO(entity, dimNames(List.of(entity)), energyTypeNames());
    }

    public PageResult<BaselineRecordVO> records(BaselineRecordQuery query) {
        LambdaQueryWrapper<EnergyBaselineDetail> wrapper = new LambdaQueryWrapper<EnergyBaselineDetail>()
                .orderByDesc(EnergyBaselineDetail::getPeriodStart);
        if (query.getBaselineId() != null) {
            wrapper.eq(EnergyBaselineDetail::getBaselineId, query.getBaselineId());
        } else if (query.getDimType() != null && query.getDimId() != null) {
            // 未指定模型时只展示当前生效模型的明细，避免历史版本产生重复记录
            LambdaQueryWrapper<EnergyBaseline> activeWrapper = new LambdaQueryWrapper<EnergyBaseline>()
                    .eq(EnergyBaseline::getDimType, query.getDimType())
                    .eq(EnergyBaseline::getDimId, query.getDimId())
                    .eq(EnergyBaseline::getStatus, 1)
                    .orderByDesc(EnergyBaseline::getId)
                    .last("LIMIT 1");
            if (query.getEnergyTypeId() != null) {
                activeWrapper.eq(EnergyBaseline::getEnergyTypeId, query.getEnergyTypeId());
            }
            EnergyBaseline active = baselineMapper.selectOne(activeWrapper);
            if (active != null) {
                wrapper.eq(EnergyBaselineDetail::getBaselineId, active.getId());
            }
        }
        if (query.getDimType() != null) {
            wrapper.eq(EnergyBaselineDetail::getDimType, query.getDimType());
        }
        if (query.getDimId() != null) {
            wrapper.eq(EnergyBaselineDetail::getDimId, query.getDimId());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(EnergyBaselineDetail::getPeriodStart, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.lt(EnergyBaselineDetail::getPeriodStart, query.getEndDate().plusDays(1).atStartOfDay());
        }
        if (Boolean.TRUE.equals(query.getOnlyDeviation())) {
            wrapper.isNotNull(EnergyBaselineDetail::getActualValue);
        }
        Page<EnergyBaselineDetail> page = detailMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<BaselineRecordVO> records = page.getRecords().stream().map(this::toRecordVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public BaselineErrorAnalysisVO errorAnalysis(Long baselineId) {
        EnergyBaseline entity = requireModel(baselineId);
        List<EnergyBaselineDetail> details = detailMapper.selectList(new LambdaQueryWrapper<EnergyBaselineDetail>()
                .eq(EnergyBaselineDetail::getBaselineId, baselineId)
                .isNotNull(EnergyBaselineDetail::getActualValue)
                .orderByAsc(EnergyBaselineDetail::getPeriodStart));

        BaselineErrorAnalysisVO vo = new BaselineErrorAnalysisVO();
        vo.setBaselineId(baselineId);
        vo.setModelVersion(entity.getModelVersion());
        vo.setSampleCount(details.size());
        vo.setFallbackCount(entity.getFallbackCount());

        if (details.isEmpty()) {
            return vo;
        }

        double sumAbs = 0;
        double sumSq = 0;
        double sumMape = 0;
        int mapeCount = 0;
        List<Double> actuals = new ArrayList<>();
        Map<String, Integer> buckets = new LinkedHashMap<>();
        String[] labels = {"<-20%", "-20%~-10%", "-10%~0", "0~10%", "10%~20%", ">20%"};
        for (String label : labels) {
            buckets.put(label, 0);
        }

        for (EnergyBaselineDetail detail : details) {
            double actual = detail.getActualValue().doubleValue();
            double baseline = detail.getBaselineValue() == null ? 0 : detail.getBaselineValue().doubleValue();
            double error = actual - baseline;
            sumAbs += Math.abs(error);
            sumSq += error * error;
            if (actual > 0) {
                sumMape += Math.abs(error) / actual;
                mapeCount++;
            }
            actuals.add(actual);
            double rate = detail.getDeviationRate() == null ? 0 : detail.getDeviationRate().doubleValue();
            buckets.merge(bucketOf(rate), 1, Integer::sum);
        }

        double mean = actuals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sst = actuals.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum();

        vo.setMae(decimal(sumAbs / details.size()));
        vo.setRmse(decimal(Math.sqrt(sumSq / details.size())));
        vo.setMape(decimal(mapeCount == 0 ? 0 : sumMape / mapeCount * 100));
        vo.setR2(decimal(sst == 0 ? 0 : 1 - sumSq / sst));
        buckets.forEach((range, count) -> vo.getBuckets().add(
                new BaselineErrorAnalysisVO.ResidualBucket(range, count)));

        List<BaselineRecordVO> worst = details.stream()
                .sorted(Comparator.comparingDouble((EnergyBaselineDetail item) ->
                        Math.abs(item.getDeviationRate() == null ? 0 : item.getDeviationRate().doubleValue())).reversed())
                .limit(10)
                .map(this::toRecordVO)
                .toList();
        vo.setWorst(new ArrayList<>(worst));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        EnergyBaseline entity = requireModel(id);
        Scope scope = new Scope(entity.getEnterpriseId(), entity.getDimType(), entity.getDimId());
        archiveActive(scope, entity.getEnergyTypeId());
        entity.setStatus(1);
        entity.setEffectiveFrom(LocalDateTime.now());
        entity.setEffectiveTo(null);
        baselineMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        EnergyBaseline entity = requireModel(id);
        entity.setStatus(2);
        entity.setEffectiveTo(LocalDateTime.now());
        baselineMapper.updateById(entity);
    }

    /* ----------------------------- 数据装载 ----------------------------- */

    private List<Bucket> loadBuckets(Scope scope, Long energyTypeId, LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        LambdaQueryWrapper<ProductionRecord> wrapper = new LambdaQueryWrapper<ProductionRecord>()
                .ge(ProductionRecord::getStartTime, from)
                .lt(ProductionRecord::getStartTime, to)
                .orderByAsc(ProductionRecord::getStartTime);
        switch (scope.dimType()) {
            case 4 -> wrapper.eq(ProductionRecord::getDeviceId, scope.dimId());
            case 3 -> wrapper.eq(ProductionRecord::getLineId, scope.dimId());
            case 2 -> wrapper.eq(ProductionRecord::getWorkshopId, scope.dimId());
            default -> wrapper.eq(ProductionRecord::getEnterpriseId, scope.enterpriseId());
        }
        List<ProductionRecord> records = productionMapper.selectList(wrapper);
        Map<LocalDateTime, Double> actuals = actualByHour(scope, energyTypeId, from, to);
        java.util.Set<LocalDateTime> badHours = badHours(scope, energyTypeId, from, to);

        Map<LocalDateTime, List<ProductionRecord>> grouped = new LinkedHashMap<>();
        for (ProductionRecord record : records) {
            LocalDateTime hour = record.getStartTime().truncatedTo(ChronoUnit.HOURS);
            grouped.computeIfAbsent(hour, key -> new ArrayList<>()).add(record);
        }

        List<Bucket> buckets = new ArrayList<>();
        grouped.forEach((hour, rows) -> {
            double output = rows.stream().mapToDouble(row -> decimalValue(row.getOutputQty())).sum();
            double run = rows.stream().mapToDouble(row -> row.getRunMinutes() == null ? 0 : row.getRunMinutes()).sum();
            double load = rows.stream()
                    .filter(row -> row.getLoadRate() != null)
                    .mapToDouble(row -> row.getLoadRate().doubleValue())
                    .average()
                    .orElse(run > 0 ? Math.min(100, run / 60.0 * 100) : 0);
            double temp = rows.stream()
                    .filter(row -> row.getAmbientTemp() != null)
                    .mapToDouble(row -> row.getAmbientTemp().doubleValue())
                    .average()
                    .orElse(20.0);
            String product = dominant(rows.stream().map(ProductionRecord::getProductType).toList());
            int shift = dominantInt(rows.stream().map(ProductionRecord::getShift).toList());
            boolean running = rows.stream().anyMatch(row -> row.getStatus() != null && row.getStatus() == 1 && row.getRunMinutes() != null && row.getRunMinutes() > 0);
            int status = running ? 1 : 2;
            Double actual = actuals.get(hour);
            boolean qualityOk = !badHours.contains(hour);
            buckets.add(new Bucket(hour, output, run, load, temp, product, shift, status, actual, qualityOk));
        });
        return buckets;
    }

    /** 质量不合格的小时：DWD 标记为可疑/重复/补录估算，训练时剔除 */
    private java.util.Set<LocalDateTime> badHours(Scope scope, Long energyTypeId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<EnergyDetail> wrapper = new LambdaQueryWrapper<EnergyDetail>()
                .eq(EnergyDetail::getEnergyTypeId, energyTypeId)
                .ge(EnergyDetail::getPeriodStart, from)
                .lt(EnergyDetail::getPeriodStart, to)
                .and(item -> item.ne(EnergyDetail::getQualityFlag, 1).or().eq(EnergyDetail::getEstimated, 1));
        switch (scope.dimType()) {
            case 4 -> wrapper.eq(EnergyDetail::getDeviceId, scope.dimId());
            case 3, 2 -> {
                List<Long> devices = deviceIds(scope);
                if (devices.isEmpty()) {
                    return java.util.Set.of();
                }
                wrapper.in(EnergyDetail::getDeviceId, devices);
            }
            default -> wrapper.eq(EnergyDetail::getEnterpriseId, scope.enterpriseId());
        }
        java.util.Set<LocalDateTime> result = new java.util.HashSet<>();
        for (EnergyDetail detail : energyDetailMapper.selectList(wrapper)) {
            result.add(detail.getPeriodStart().truncatedTo(ChronoUnit.HOURS));
        }
        return result;
    }

    private Map<LocalDateTime, Double> actualByHour(Scope scope, Long energyTypeId, LocalDateTime from, LocalDateTime to) {
        QueryWrapper<com.smarten.module.datalake.entity.EnergyAggregate> wrapper = new QueryWrapper<>();
        wrapper.select("period_start", "SUM(energy_value) AS energy_value")
                .eq("deleted", 0)
                .eq("energy_type_id", energyTypeId)
                .eq("period_type", 1)
                .eq("dim_type", scope.dimType())
                .eq("dim_id", scope.dimId())
                .ge("period_start", from)
                .lt("period_start", to)
                .groupBy("period_start");
        Map<LocalDateTime, Double> result = new HashMap<>();
        for (Map<String, Object> row : aggregateMapper.selectMaps(wrapper)) {
            LocalDateTime hour = toDateTime(row.get("period_start"));
            if (hour == null) {
                continue;
            }
            result.merge(hour, row.get("energy_value") == null ? 0
                    : ((Number) row.get("energy_value")).doubleValue(), Double::sum);
        }
        return result;
    }

    private LocalDateTime toDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
    }

    /* ------------------------------- 辅助 ------------------------------- */

    private record Scope(Long enterpriseId, int dimType, Long dimId) {
    }

    private record Bucket(LocalDateTime hour, double output, double run, double load, double temp,
                          String product, int shift, int status, Double actual, boolean qualityOk) {

        Sample toSample(Long deviceId) {
            return new Sample(deviceId, hour, output, run, load, temp, product, shift, status,
                    actual == null ? 0 : actual);
        }

        Map<String, Object> condition() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("outputQty", round(output));
            map.put("runMinutes", round(run));
            map.put("loadRate", round(load));
            map.put("ambientTemp", round(temp));
            map.put("productType", product);
            map.put("shift", shift);
            map.put("status", status);
            return map;
        }
    }

    private Scope resolveScope(Integer dimType, Long dimId) {
        int type = dimType == null ? 4 : dimType;
        Long enterpriseId = switch (type) {
            case 4 -> {
                EnergyDevice device = deviceMapper.selectById(dimId);
                if (device == null) {
                    throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
                }
                yield device.getEnterpriseId();
            }
            case 3 -> {
                EnergyLine line = lineMapper.selectById(dimId);
                if (line == null) {
                    throw new BusinessException(ResultCode.LINE_NOT_FOUND);
                }
                yield line.getEnterpriseId();
            }
            case 2 -> {
                EnergyWorkshop workshop = workshopMapper.selectById(dimId);
                if (workshop == null) {
                    throw new BusinessException(ResultCode.WORKSHOP_NOT_FOUND);
                }
                yield workshop.getEnterpriseId();
            }
            default -> {
                EnergyEnterprise enterprise = enterpriseMapper.selectById(dimId);
                if (enterprise == null) {
                    throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
                }
                yield enterprise.getId();
            }
        };
        dataScope.checkAccessible(enterpriseId);
        return new Scope(enterpriseId, type, dimId);
    }

    private List<Long> deviceIds(Scope scope) {
        LambdaQueryWrapper<EnergyDevice> wrapper = new LambdaQueryWrapper<>();
        if (scope.dimType() == 3) {
            wrapper.eq(EnergyDevice::getLineId, scope.dimId());
        } else if (scope.dimType() == 2) {
            wrapper.eq(EnergyDevice::getWorkshopId, scope.dimId());
        } else {
            wrapper.eq(EnergyDevice::getEnterpriseId, scope.enterpriseId());
        }
        return deviceMapper.selectList(wrapper).stream().map(EnergyDevice::getId).toList();
    }

    private EnergyBaseline requireModel(Long id) {
        EnergyBaseline entity = baselineMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "基线模型不存在");
        }
        dataScope.checkAccessible(entity.getEnterpriseId());
        return entity;
    }

    private EnergyBaseline activeModel(Scope scope, Long energyTypeId) {
        return baselineMapper.selectOne(new LambdaQueryWrapper<EnergyBaseline>()
                .eq(EnergyBaseline::getDimType, scope.dimType())
                .eq(EnergyBaseline::getDimId, scope.dimId())
                .eq(EnergyBaseline::getEnergyTypeId, energyTypeId)
                .eq(EnergyBaseline::getStatus, 1)
                .orderByDesc(EnergyBaseline::getId)
                .last("LIMIT 1"));
    }

    private void archiveActive(Scope scope, Long energyTypeId) {
        List<EnergyBaseline> active = baselineMapper.selectList(new LambdaQueryWrapper<EnergyBaseline>()
                .eq(EnergyBaseline::getDimType, scope.dimType())
                .eq(EnergyBaseline::getDimId, scope.dimId())
                .eq(EnergyBaseline::getEnergyTypeId, energyTypeId)
                .eq(EnergyBaseline::getStatus, 1));
        for (EnergyBaseline item : active) {
            item.setStatus(2);
            item.setEffectiveTo(LocalDateTime.now());
            baselineMapper.updateById(item);
        }
    }

    private String nextVersion(Scope scope, Long energyTypeId) {
        Long count = baselineMapper.selectCount(new LambdaQueryWrapper<EnergyBaseline>()
                .eq(EnergyBaseline::getDimType, scope.dimType())
                .eq(EnergyBaseline::getDimId, scope.dimId())
                .eq(EnergyBaseline::getEnergyTypeId, energyTypeId));
        return "v" + (count + 1) + "-" + LocalDateTime.now().format(VERSION_FORMAT);
    }

    private int toBaselineType(String algo) {
        return switch (algo) {
            case BaselineModel.RIDGE -> DETAIL_TYPE_REGRESSION;
            case BaselineModel.GROUP_MEAN -> DETAIL_TYPE_GROUP_MEAN;
            default -> DETAIL_TYPE_HISTORICAL;
        };
    }

    private BaselineModel readModel(EnergyBaseline entity) {
        Map<String, Object> params;
        try {
            params = objectMapper.readValue(entity.getParamsJson(), new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "模型参数损坏，请重新训练");
        }
        BaselineModel model = new BaselineModel();
        model.setAlgo(String.valueOf(params.getOrDefault("algo", BaselineModel.HISTORICAL)));
        Object beta = params.get("beta");
        if (beta instanceof List<?> list && !list.isEmpty()) {
            double[] values = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                values[i] = ((Number) list.get(i)).doubleValue();
            }
            model.setBeta(values);
        }
        Object names = params.get("featureNames");
        if (names instanceof List<?> list) {
            model.setFeatureNames(list.stream().map(String::valueOf).toArray(String[]::new));
        } else {
            model.setFeatureNames(new String[0]);
        }
        Object products = params.get("productTypes");
        if (products instanceof List<?> list) {
            model.setProductTypes(list.stream().map(String::valueOf).toList());
        }
        Object shifts = params.get("shifts");
        if (shifts instanceof List<?> list) {
            model.setShifts(list.stream().map(item -> ((Number) item).intValue()).toList());
        }
        Object groupMeans = params.get("groupMeans");
        if (groupMeans instanceof Map<?, ?> map) {
            Map<String, Double> means = new LinkedHashMap<>();
            map.forEach((key, value) -> means.put(String.valueOf(key), ((Number) value).doubleValue()));
            model.setGroupMeans(means);
        }
        model.setStandbyBaseline(number(params.get("standbyBaseline")));
        model.setHistoricalBaseline(number(params.get("historicalBaseline")));
        model.setSampleCount((int) number(params.get("sampleCount")));
        model.setStandbySamples((int) number(params.get("standbySamples")));
        model.setOutlierCount((int) number(params.get("outlierCount")));
        model.setRejectedCount((int) number(params.get("rejectedCount")));
        model.setMae(number(params.get("mae")));
        model.setRmse(number(params.get("rmse")));
        model.setMape(number(params.get("mape")));
        model.setR2(number(params.get("r2")));
        Object fallbackLog = params.get("fallbackLog");
        if (fallbackLog instanceof List<?> list) {
            model.setFallbackLog(list.stream().map(String::valueOf).toList());
        }
        return model;
    }

    private BaselineModelVO toModelVO(EnergyBaseline entity, Map<Long, String> dimNames,
                                      Map<Long, String> typeNames) {
        BaselineModelVO vo = new BaselineModelVO();
        vo.setId(entity.getId());
        vo.setEnterpriseId(entity.getEnterpriseId());
        vo.setDimType(entity.getDimType());
        vo.setDimId(entity.getDimId());
        vo.setDimName(dimNames.getOrDefault(entity.getDimId(), "—"));
        vo.setEnergyTypeId(entity.getEnergyTypeId());
        vo.setEnergyTypeName(typeNames.getOrDefault(entity.getEnergyTypeId(), "—"));
        vo.setModelVersion(entity.getModelVersion());
        vo.setAlgo(entity.getAlgo());
        vo.setBaselineType(entity.getBaselineType());
        vo.setSampleCount(entity.getSampleCount());
        vo.setSampleRejected(entity.getSampleRejected());
        vo.setOutlierCount(entity.getOutlierCount());
        vo.setFallbackCount(entity.getFallbackCount());
        vo.setMae(entity.getMae());
        vo.setRmse(entity.getRmse());
        vo.setMape(entity.getMape());
        vo.setR2(entity.getR2());
        vo.setConfidence(entity.getConfidence());
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(statusLabel(entity.getStatus()));
        vo.setTrainStart(entity.getTrainStart());
        vo.setTrainEnd(entity.getTrainEnd());
        vo.setEffectiveFrom(entity.getEffectiveFrom());
        vo.setEffectiveTo(entity.getEffectiveTo());
        vo.setCalcTime(entity.getCalcTime());
        Map<String, Object> params = readParams(entity.getParamsJson());
        vo.setParams(params);
        Object names = params.get("featureNames");
        if (names instanceof List<?> list) {
            vo.setFeatureNames(list.stream().map(String::valueOf).toList());
        }
        Object log = params.get("fallbackLog");
        if (log instanceof List<?> list) {
            vo.setFallbackLog(list.stream().map(String::valueOf).toList());
        }
        Object standby = params.get("standbySamples");
        if (standby instanceof Number number) {
            vo.setStandbySamples(number.intValue());
        }
        return vo;
    }

    private BaselineRecordVO toRecordVO(EnergyBaselineDetail detail) {
        BaselineRecordVO vo = new BaselineRecordVO();
        vo.setId(detail.getId());
        vo.setBaselineId(detail.getBaselineId());
        vo.setModelVersion(detail.getModelVersion());
        vo.setPeriodStart(detail.getPeriodStart());
        vo.setPeriodEnd(detail.getPeriodEnd());
        vo.setActualValue(detail.getActualValue());
        vo.setBaselineValue(detail.getBaselineValue());
        vo.setDeviationValue(detail.getDeviationValue());
        vo.setDeviationRate(detail.getDeviationRate());
        vo.setBaselineType(detail.getBaselineType());
        vo.setBaselineTypeName(Prediction.typeName(detail.getBaselineType() == null
                ? DETAIL_TYPE_HISTORICAL : detail.getBaselineType()));
        vo.setFallbackReason(detail.getFallbackReason());
        vo.setComparable(detail.getComparable());
        vo.setCondition(readParams(detail.getConditionJson()));
        return vo;
    }

    private Map<Long, String> dimNames(List<EnergyBaseline> models) {
        Map<Long, String> names = new HashMap<>();
        for (Integer dimType : List.of(1, 2, 3, 4)) {
            List<Long> ids = models.stream()
                    .filter(item -> Objects.equals(item.getDimType(), dimType))
                    .map(EnergyBaseline::getDimId)
                    .distinct()
                    .toList();
            if (ids.isEmpty()) {
                continue;
            }
            switch (dimType) {
                case 1 -> enterpriseMapper.selectBatchIds(ids)
                        .forEach(item -> names.put(item.getId(), item.getName()));
                case 2 -> workshopMapper.selectBatchIds(ids)
                        .forEach(item -> names.put(item.getId(), item.getName()));
                case 3 -> lineMapper.selectBatchIds(ids)
                        .forEach(item -> names.put(item.getId(), item.getName()));
                default -> deviceMapper.selectBatchIds(ids)
                        .forEach(item -> names.put(item.getId(), item.getName()));
            }
        }
        return names;
    }

    private Map<Long, String> energyTypeNames() {
        return energyTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(EnergyType::getId, EnergyType::getName, (a, b) -> a));
    }

    private String dominant(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    private int dominantInt(List<Integer> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1);
    }

    private String bucketOf(double rate) {
        if (rate < -20) {
            return "<-20%";
        }
        if (rate < -10) {
            return "-20%~-10%";
        }
        if (rate < 0) {
            return "-10%~0";
        }
        if (rate <= 10) {
            return "0~10%";
        }
        if (rate <= 20) {
            return "10%~20%";
        }
        return ">20%";
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "生效中";
            case 2 -> "已归档";
            default -> "草稿";
        };
    }

    private Map<String, Object> readParams(String json) {
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    private double decimalValue(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static double round(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.round(value * 10000.0) / 10000.0;
    }
}
