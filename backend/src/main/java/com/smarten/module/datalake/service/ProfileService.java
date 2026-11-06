package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.datalake.entity.EnergyAggregate;
import com.smarten.module.datalake.entity.ProductionRecord;
import com.smarten.module.datalake.mapper.EnergyAggregateMapper;
import com.smarten.module.datalake.mapper.ProfileExtrasMapper;
import com.smarten.module.datalake.mapper.ProductionRecordMapper;
import com.smarten.module.datalake.vo.AnomalySliceVO;
import com.smarten.module.datalake.vo.BaselinePointVO;
import com.smarten.module.datalake.vo.CockpitVO;
import com.smarten.module.datalake.vo.DeviceProfileVO;
import com.smarten.module.datalake.vo.EnergySliceVO;
import com.smarten.module.datalake.vo.EnterpriseProfileVO;
import com.smarten.module.datalake.vo.HierarchyNodeVO;
import com.smarten.module.datalake.vo.KpiCardVO;
import com.smarten.module.datalake.vo.LineProfileVO;
import com.smarten.module.datalake.vo.LineRankVO;
import com.smarten.module.datalake.vo.ProfilePointVO;
import com.smarten.module.datalake.vo.RatingVO;
import com.smarten.module.datalake.vo.SankeyGraphVO;
import com.smarten.module.datalake.vo.SavingPointVO;
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
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 画像与驾驶舱（ADS 层）：只读聚合结果，不直接扫描明细。
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int PERIOD_HOUR = 1;
    private static final int PERIOD_DAY = 2;
    private static final int PERIOD_MONTH = 3;

    private static final int DIM_ENTERPRISE = 1;
    private static final int DIM_WORKSHOP = 2;
    private static final int DIM_LINE = 3;
    private static final int DIM_DEVICE = 4;

    private final EnergyAggregateMapper aggregateMapper;
    private final ProductionRecordMapper productionMapper;
    private final ProfileExtrasMapper extrasMapper;
    private final EnergyEnterpriseMapper enterpriseMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyMeterMapper meterMapper;
    private final EnergyTypeMapper energyTypeMapper;
    private final DatalakeCompletenessService completenessService;
    private final DataScopeHelper dataScope;

    /* ----------------------------- 驾驶舱 ----------------------------- */

    public CockpitVO cockpit(LocalDate date, Integer scopeType, Long scopeId, Long energyTypeId, String granularity) {
        LocalDate day = date == null ? LocalDate.now() : date;
        Scope scope = resolveScope(scopeType, scopeId);
        Map<Long, EnergyType> types = energyTypes();
        CockpitVO vo = new CockpitVO();
        vo.setDate(day);
        vo.setGranularity(granularity == null ? "hour" : granularity);
        vo.setScopeType(scope.type());
        vo.setScopeId(scope.id());
        vo.setScopeName(scope.name());
        vo.setEnergyTypeId(energyTypeId);

        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Map<String, Object>> byType = groupByEnergyType(scope.type(), scope.id(), dayStart, dayEnd, energyTypeId);
        BigDecimal totalEnergy = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal coal = BigDecimal.ZERO;
        for (Map<String, Object> row : byType) {
            BigDecimal energy = toDecimal(row.get("energy_value"));
            totalEnergy = totalEnergy.add(energy);
            totalCost = totalCost.add(toDecimal(row.get("cost_amount")));
            EnergyType type = types.get(toLong(row.get("energy_type_id")));
            if (type != null && type.getCoalFactor() != null) {
                coal = coal.add(energy.multiply(type.getCoalFactor()).divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP));
            }
            vo.getStructure().add(slice(type, energy, toDecimal(row.get("cost_amount"))));
        }
        vo.getStructure().sort((a, b) -> b.getEnergyValue().compareTo(a.getEnergyValue()));

        boolean hourly = "hour".equalsIgnoreCase(vo.getGranularity());
        if (hourly) {
            vo.setTrend(trend(scope.type(), scope.id(), PERIOD_HOUR, dayStart, dayEnd, energyTypeId, true));
        } else {
            LocalDateTime start = day.minusDays(13).atStartOfDay();
            vo.setTrend(trend(scope.type(), scope.id(), PERIOD_DAY, start, dayEnd, energyTypeId, false));
        }

        BigDecimal completeness = completenessService.compute(completenessQuery(scope.enterpriseId(), day)).getCompletenessRate();

        vo.getKpis().add(kpi("energy", "今日总能耗", totalEnergy, "kWh", true, null));
        vo.getKpis().add(kpi("coal", "综合能耗", coal, "tce", true, "按折标煤系数换算"));
        vo.getKpis().add(kpi("cost", "能源成本", totalCost, "元", true, null));
        vo.getKpis().add(kpi("completeness", "数据完整率", completeness, "%", true, "应采/有效入库"));
        vo.getKpis().add(kpi("deviation", "能耗偏差率", null, "%", false, "动态基线 P5 接入后可用"));
        vo.getKpis().add(kpi("anomaly", "异常设备数", null, "台", false, "异常诊断 P6 接入后可用"));
        vo.getKpis().add(kpi("task", "待整改任务", null, "项", false, "整改任务 P7 接入后可用"));
        vo.getKpis().add(kpi("saving", "累计节能量", null, "kWh", false, "节能核算 P8 接入后可用"));
        vo.getUnavailable().addAll(List.of("deviation", "anomaly", "task", "saving"));

        vo.setLineRanking(lineRanking(scope.enterpriseId(), day, energyTypeId));
        vo.setBaseline(baselinePoints(scope, day));
        vo.setAnomaly(anomalyDistribution(scope.enterpriseId(), day));
        vo.setSaving(savingTrend(scope.enterpriseId()));
        vo.setFlow(flow(scope, day));
        return vo;
    }

    /* --------------------------- 企业画像 --------------------------- */

    public EnterpriseProfileVO enterpriseProfile(Long id, Integer days, Long energyTypeId) {
        int range = normalizeDays(days);
        Scope scope = resolveScope(DIM_ENTERPRISE, id == null ? dataScope.requireEnterpriseId() : id);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(range - 1L);
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();

        Map<Long, EnergyType> types = energyTypes();
        List<EnergyAggregate> rows = aggregates(scope.type(), scope.id(), PERIOD_DAY, startTime, endTime, energyTypeId);
        Map<LocalDate, BigDecimal> daily = new HashMap<>();
        Map<Long, BigDecimal> byType = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal coal = BigDecimal.ZERO;
        for (EnergyAggregate row : rows) {
            BigDecimal value = nz(row.getEnergyValue());
            total = total.add(value);
            cost = cost.add(nz(row.getCostAmount()));
            daily.merge(row.getPeriodStart().toLocalDate(), value, BigDecimal::add);
            byType.merge(row.getEnergyTypeId(), value, BigDecimal::add);
            EnergyType type = types.get(row.getEnergyTypeId());
            if (type != null && type.getCoalFactor() != null) {
                coal = coal.add(value.multiply(type.getCoalFactor()).divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP));
            }
        }

        EnterpriseProfileVO vo = new EnterpriseProfileVO();
        vo.setId(scope.id());
        vo.setName(scope.name());
        vo.setDays(range);
        EnergyEnterprise enterprise = enterpriseMapper.selectById(scope.id());
        if (enterprise != null) {
            vo.setIndustry(enterprise.getIndustry());
            vo.setRegion(enterprise.getRegion());
        }
        vo.setTotalEnergy(scale2(total));
        vo.setTotalCost(scale2(cost));
        vo.setCoalConsumption(scale4(coal));
        vo.setCompletenessRate(completenessService.compute(completenessQuery(scope.enterpriseId(), end)).getCompletenessRate());

        Map<String, Object> production = productionSum("enterprise_id", scope.id(), start, end);
        BigDecimal output = toDecimal(production.get("output_qty"));
        vo.setOutputTotal(output);
        vo.setOutputUnit(output.signum() > 0 ? "件" : null);
        if (output.signum() > 0) {
            vo.setUnitProductEnergy(total.divide(output, 4, RoundingMode.HALF_UP));
        } else {
            vo.getUnavailable().add("unitProductEnergy");
        }
        vo.getUnavailable().add("energyIntensity");

        vo.setStructure(slices(byType, types));
        vo.setTrend(series(start, end, daily, false));
        vo.setRating(rating(vo.getCompletenessRate(), daily));
        return vo;
    }

    /* --------------------------- 产线画像 --------------------------- */

    public LineProfileVO lineProfile(Long id, Integer days) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少产线 ID");
        }
        int range = normalizeDays(days);
        EnergyLine line = lineMapper.selectById(id);
        if (line == null) {
            throw new BusinessException(ResultCode.LINE_NOT_FOUND);
        }
        dataScope.checkAccessible(line.getEnterpriseId());
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(range - 1L);
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();

        Map<Long, EnergyType> types = energyTypes();
        List<EnergyAggregate> rows = aggregates(DIM_LINE, id, PERIOD_DAY, startTime, endTime, null);
        Map<LocalDate, BigDecimal> daily = new HashMap<>();
        Map<Long, BigDecimal> byType = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (EnergyAggregate row : rows) {
            BigDecimal value = nz(row.getEnergyValue());
            total = total.add(value);
            daily.merge(row.getPeriodStart().toLocalDate(), value, BigDecimal::add);
            byType.merge(row.getEnergyTypeId(), value, BigDecimal::add);
        }

        LineProfileVO vo = new LineProfileVO();
        vo.setId(line.getId());
        vo.setCode(line.getCode());
        vo.setName(line.getName());
        vo.setDays(range);
        EnergyWorkshop workshop = line.getWorkshopId() == null ? null : workshopMapper.selectById(line.getWorkshopId());
        vo.setWorkshopName(workshop == null ? null : workshop.getName());
        EnergyEnterprise enterprise = enterpriseMapper.selectById(line.getEnterpriseId());
        vo.setEnterpriseName(enterprise == null ? null : enterprise.getName());
        vo.setEnergy(scale2(total));

        Map<String, Object> production = productionSum("line_id", id, start, end);
        BigDecimal output = toDecimal(production.get("output_qty"));
        vo.setOutputQty(output);
        vo.setOutputUnit(output.signum() > 0 ? "件" : null);
        vo.setRunMinutes(toInt(production.get("run_minutes")));
        if (output.signum() > 0) {
            vo.setUnitProductEnergy(total.divide(output, 4, RoundingMode.HALF_UP));
        } else {
            vo.getUnavailable().add("unitProductEnergy");
        }

        Long deviceCount = deviceMapper.selectCount(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getLineId, id));
        Long activeCount = deviceMapper.selectCount(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getLineId, id).eq(EnergyDevice::getStatus, 1));
        vo.setDeviceCount(deviceCount == null ? 0 : deviceCount.intValue());
        vo.setActiveDeviceCount(activeCount == null ? 0 : activeCount.intValue());
        vo.getUnavailable().addAll(List.of("baseline", "deviationRate", "anomalyDeviceCount"));

        vo.setStructure(slices(byType, types));
        vo.setTrend(series(start, end, daily, false));
        return vo;
    }

    /* --------------------------- 设备画像 --------------------------- */

    public DeviceProfileVO deviceProfile(Long id, Integer days) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少设备 ID");
        }
        int range = normalizeDays(days);
        EnergyDevice device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        dataScope.checkAccessible(device.getEnterpriseId());
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(range - 1L);

        Map<Long, EnergyType> types = energyTypes();
        List<EnergyAggregate> rows = aggregates(DIM_DEVICE, id, PERIOD_DAY, start.atStartOfDay(),
                end.plusDays(1).atStartOfDay(), null);
        Map<LocalDate, BigDecimal> daily = new HashMap<>();
        Map<Long, BigDecimal> byType = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (EnergyAggregate row : rows) {
            BigDecimal value = nz(row.getEnergyValue());
            total = total.add(value);
            daily.merge(row.getPeriodStart().toLocalDate(), value, BigDecimal::add);
            byType.merge(row.getEnergyTypeId(), value, BigDecimal::add);
        }

        DeviceProfileVO vo = new DeviceProfileVO();
        vo.setId(device.getId());
        vo.setCode(device.getCode());
        vo.setName(device.getName());
        vo.setDeviceType(device.getDeviceType());
        vo.setModel(device.getModel());
        vo.setRatedPower(device.getRatedPower());
        vo.setStatus(device.getStatus());
        vo.setCommissionDate(device.getCommissionDate());
        vo.setDays(range);
        vo.setEnergy(scale2(total));

        EnergyLine line = device.getLineId() == null ? null : lineMapper.selectById(device.getLineId());
        vo.setLineName(line == null ? null : line.getName());
        EnergyWorkshop workshop = device.getWorkshopId() == null ? null : workshopMapper.selectById(device.getWorkshopId());
        vo.setWorkshopName(workshop == null ? null : workshop.getName());
        EnergyEnterprise enterprise = enterpriseMapper.selectById(device.getEnterpriseId());
        vo.setEnterpriseName(enterprise == null ? null : enterprise.getName());

        Map<String, Object> production = productionSum("device_id", id, start, end);
        BigDecimal output = toDecimal(production.get("output_qty"));
        vo.setOutputQty(output);
        vo.setRunMinutes(toInt(production.get("run_minutes")));
        vo.setLoadRate(scale2(toDecimal(production.get("load_rate"))));
        if (output.signum() > 0) {
            vo.setUnitProductEnergy(total.divide(output, 4, RoundingMode.HALF_UP));
        } else {
            vo.getUnavailable().add("unitProductEnergy");
        }
        vo.getUnavailable().add("anomalyHistory");

        vo.setStructure(slices(byType, types));
        vo.setTrend(series(start, end, daily, false));
        return vo;
    }

    /* --------------------------- 层级树 --------------------------- */

    public List<HierarchyNodeVO> hierarchy(Long enterpriseId) {
        Long scope = dataScope.resolveEnterpriseId(enterpriseId);
        LambdaQueryWrapper<EnergyEnterprise> wrapper = new LambdaQueryWrapper<>();
        if (scope != null) {
            wrapper.eq(EnergyEnterprise::getId, scope);
        }
        wrapper.eq(EnergyEnterprise::getStatus, 1).orderByAsc(EnergyEnterprise::getId);
        List<EnergyEnterprise> enterprises = enterpriseMapper.selectList(wrapper);
        List<HierarchyNodeVO> result = new ArrayList<>();
        for (EnergyEnterprise enterprise : enterprises) {
            HierarchyNodeVO node = new HierarchyNodeVO();
            node.setId(enterprise.getId());
            node.setType("enterprise");
            node.setName(enterprise.getName());
            node.setCode(enterprise.getCode());
            node.setChildren(buildWorkshops(enterprise.getId()));
            result.add(node);
        }
        return result;
    }

    private List<HierarchyNodeVO> buildWorkshops(Long enterpriseId) {
        List<EnergyWorkshop> workshops = workshopMapper.selectList(new LambdaQueryWrapper<EnergyWorkshop>()
                .eq(EnergyWorkshop::getEnterpriseId, enterpriseId).orderByAsc(EnergyWorkshop::getId));
        List<HierarchyNodeVO> nodes = new ArrayList<>();
        for (EnergyWorkshop workshop : workshops) {
            HierarchyNodeVO node = new HierarchyNodeVO();
            node.setId(workshop.getId());
            node.setType("workshop");
            node.setName(workshop.getName());
            node.setCode(workshop.getCode());
            node.setChildren(buildLines(workshop.getId()));
            nodes.add(node);
        }
        return nodes;
    }

    private List<HierarchyNodeVO> buildLines(Long workshopId) {
        List<EnergyLine> lines = lineMapper.selectList(new LambdaQueryWrapper<EnergyLine>()
                .eq(EnergyLine::getWorkshopId, workshopId).orderByAsc(EnergyLine::getId));
        List<HierarchyNodeVO> nodes = new ArrayList<>();
        for (EnergyLine line : lines) {
            HierarchyNodeVO node = new HierarchyNodeVO();
            node.setId(line.getId());
            node.setType("line");
            node.setName(line.getName());
            node.setCode(line.getCode());
            node.setChildren(buildDevices(line.getId()));
            nodes.add(node);
        }
        return nodes;
    }

    private List<HierarchyNodeVO> buildDevices(Long lineId) {
        List<EnergyDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getLineId, lineId).orderByAsc(EnergyDevice::getId));
        List<HierarchyNodeVO> nodes = new ArrayList<>();
        for (EnergyDevice device : devices) {
            HierarchyNodeVO node = new HierarchyNodeVO();
            node.setId(device.getId());
            node.setType("device");
            node.setName(device.getName());
            node.setCode(device.getCode());
            nodes.add(node);
        }
        return nodes;
    }

    /* --------------------------- 辅助 --------------------------- */

    private record Scope(int type, Long id, String name, Long enterpriseId) {
    }

    private Scope resolveScope(Integer type, Long id) {
        int scopeType = type == null ? DIM_ENTERPRISE : type;
        Long scopeId = id;
        if (scopeId == null) {
            Long own = dataScope.currentEnterpriseId();
            if (own != null) {
                scopeType = DIM_ENTERPRISE;
                scopeId = own;
            } else {
                List<EnergyEnterprise> list = enterpriseMapper.selectList(new LambdaQueryWrapper<EnergyEnterprise>()
                        .eq(EnergyEnterprise::getStatus, 1).orderByAsc(EnergyEnterprise::getId).last("LIMIT 1"));
                if (list.isEmpty()) {
                    throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND, "尚无企业数据");
                }
                scopeType = DIM_ENTERPRISE;
                scopeId = list.get(0).getId();
            }
        }

        Long enterpriseId;
        String name;
        switch (scopeType) {
            case DIM_WORKSHOP -> {
                EnergyWorkshop workshop = workshopMapper.selectById(scopeId);
                if (workshop == null) {
                    throw new BusinessException(ResultCode.WORKSHOP_NOT_FOUND);
                }
                enterpriseId = workshop.getEnterpriseId();
                name = workshop.getName();
            }
            case DIM_LINE -> {
                EnergyLine line = lineMapper.selectById(scopeId);
                if (line == null) {
                    throw new BusinessException(ResultCode.LINE_NOT_FOUND);
                }
                enterpriseId = line.getEnterpriseId();
                name = line.getName();
            }
            case DIM_DEVICE -> {
                EnergyDevice device = deviceMapper.selectById(scopeId);
                if (device == null) {
                    throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
                }
                enterpriseId = device.getEnterpriseId();
                name = device.getName();
            }
            default -> {
                EnergyEnterprise enterprise = enterpriseMapper.selectById(scopeId);
                if (enterprise == null) {
                    throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
                }
                enterpriseId = enterprise.getId();
                name = enterprise.getName();
            }
        }
        dataScope.checkAccessible(enterpriseId);
        return new Scope(scopeType, scopeId, name, enterpriseId);
    }

    private Map<Long, EnergyType> energyTypes() {
        return energyTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(EnergyType::getId, t -> t, (a, b) -> a));
    }

    private List<EnergyAggregate> aggregates(int dimType, Long dimId, int periodType,
                                             LocalDateTime start, LocalDateTime end, Long energyTypeId) {
        LambdaQueryWrapper<EnergyAggregate> wrapper = new LambdaQueryWrapper<EnergyAggregate>()
                .eq(EnergyAggregate::getDimType, dimType)
                .eq(EnergyAggregate::getDimId, dimId)
                .eq(EnergyAggregate::getPeriodType, periodType)
                .ge(EnergyAggregate::getPeriodStart, start)
                .lt(EnergyAggregate::getPeriodStart, end)
                .orderByAsc(EnergyAggregate::getPeriodStart);
        if (energyTypeId != null) {
            wrapper.eq(EnergyAggregate::getEnergyTypeId, energyTypeId);
        }
        return aggregateMapper.selectList(wrapper);
    }

    private List<Map<String, Object>> groupByEnergyType(int dimType, Long dimId,
                                                        LocalDateTime start, LocalDateTime end, Long energyTypeId) {
        QueryWrapper<EnergyAggregate> wrapper = new QueryWrapper<>();
        wrapper.select("energy_type_id", "SUM(energy_value) AS energy_value", "SUM(cost_amount) AS cost_amount")
                .eq("deleted", 0)
                .eq("dim_type", dimType)
                .eq("dim_id", dimId)
                .eq("period_type", PERIOD_DAY)
                .ge("period_start", start)
                .lt("period_start", end);
        if (energyTypeId != null) {
            wrapper.eq("energy_type_id", energyTypeId);
        }
        wrapper.groupBy("energy_type_id");
        return aggregateMapper.selectMaps(wrapper);
    }

    private List<ProfilePointVO> trend(int dimType, Long dimId, int periodType,
                                       LocalDateTime start, LocalDateTime end,
                                       Long energyTypeId, boolean hourly) {
        QueryWrapper<EnergyAggregate> wrapper = new QueryWrapper<>();
        wrapper.select("period_start", "SUM(energy_value) AS energy_value")
                .eq("deleted", 0)
                .eq("dim_type", dimType)
                .eq("dim_id", dimId)
                .eq("period_type", periodType)
                .ge("period_start", start)
                .lt("period_start", end);
        if (energyTypeId != null) {
            wrapper.eq("energy_type_id", energyTypeId);
        }
        wrapper.groupBy("period_start").orderByAsc("period_start");

        List<ProfilePointVO> points = new ArrayList<>();
        for (Map<String, Object> row : aggregateMapper.selectMaps(wrapper)) {
            LocalDateTime time = toDateTime(row.get("period_start"));
            if (time == null) {
                continue;
            }
            ProfilePointVO point = new ProfilePointVO();
            point.setTime(time);
            point.setLabel(hourly ? String.format("%02d:00", time.getHour()) : time.toLocalDate().toString());
            point.setEnergyValue(scale2(toDecimal(row.get("energy_value"))));
            points.add(point);
        }
        return points;
    }

    private List<LineRankVO> lineRanking(Long enterpriseId, LocalDate day, Long energyTypeId) {
        QueryWrapper<EnergyAggregate> wrapper = new QueryWrapper<>();
        wrapper.select("dim_id", "SUM(energy_value) AS energy_value")
                .eq("deleted", 0)
                .eq("dim_type", DIM_LINE)
                .eq("period_type", PERIOD_DAY)
                .eq("period_start", day.atStartOfDay());
        if (energyTypeId != null) {
            wrapper.eq("energy_type_id", energyTypeId);
        }
        wrapper.groupBy("dim_id").orderByDesc("energy_value");

        List<Map<String, Object>> rows = aggregateMapper.selectMaps(wrapper);
        Map<Long, String> names = lineNames(rows.stream().map(r -> toLong(r.get("dim_id"))).toList());
        List<LineRankVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long lineId = toLong(row.get("dim_id"));
            if (lineId == null) {
                continue;
            }
            LineRankVO vo = new LineRankVO();
            vo.setLineId(lineId);
            vo.setLineName(names.getOrDefault(lineId, "产线" + lineId));
            vo.setEnergyValue(scale2(toDecimal(row.get("energy_value"))));
            result.add(vo);
        }
        return result;
    }

    private List<BaselinePointVO> baselinePoints(Scope scope, LocalDate day) {
        List<BaselinePointVO> result = new ArrayList<>();
        for (Map<String, Object> row : extrasMapper.baselinePoints(scope.type(), scope.id(),
                day.atStartOfDay(), day.plusDays(1).atStartOfDay())) {
            BaselinePointVO point = new BaselinePointVO();
            point.setLabel((String) row.get("label"));
            point.setBaseline(toDecimal(row.get("baseline")));
            point.setActual(toDecimal(row.get("actual")));
            point.setDeviationRate(toDecimal(row.get("deviation")));
            result.add(point);
        }
        return result;
    }

    private List<AnomalySliceVO> anomalyDistribution(Long enterpriseId, LocalDate day) {
        List<AnomalySliceVO> result = new ArrayList<>();
        List<Map<String, Object>> rows = extrasMapper.anomalyDistribution(enterpriseId,
                day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        int total = rows.stream().mapToInt(r -> toInt(r.get("cnt"))).sum();
        for (Map<String, Object> row : rows) {
            AnomalySliceVO slice = new AnomalySliceVO();
            slice.setName((String) row.get("name"));
            slice.setCount(toInt(row.get("cnt")));
            slice.setRatio(total == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(slice.getCount() * 100.0 / total).setScale(2, RoundingMode.HALF_UP));
            result.add(slice);
        }
        return result;
    }

    private List<SavingPointVO> savingTrend(Long enterpriseId) {
        List<SavingPointVO> result = new ArrayList<>();
        for (Map<String, Object> row : extrasMapper.savingTrend(enterpriseId,
                LocalDate.now().minusMonths(5).withDayOfMonth(1).atStartOfDay())) {
            SavingPointVO point = new SavingPointVO();
            point.setMonth((String) row.get("month"));
            point.setSavingEnergy(scale2(toDecimal(row.get("saving_energy"))));
            point.setSavingAmount(scale2(toDecimal(row.get("saving_amount"))));
            result.add(point);
        }
        return result;
    }

    private SankeyGraphVO flow(Scope scope, LocalDate day) {
        SankeyGraphVO graph = new SankeyGraphVO();
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        if (scope.type() != DIM_ENTERPRISE) {
            return graph;
        }
        Map<Long, BigDecimal> workshops = groupEnergy(DIM_WORKSHOP, start, end);
        Map<Long, BigDecimal> lines = groupEnergy(DIM_LINE, start, end);
        Map<Long, BigDecimal> devices = groupEnergy(DIM_DEVICE, start, end);
        if (workshops.isEmpty() && lines.isEmpty()) {
            return graph;
        }

        Map<Long, EnergyWorkshop> workshopMap = mapById(workshopMapper.selectList(new LambdaQueryWrapper<EnergyWorkshop>()
                .eq(EnergyWorkshop::getEnterpriseId, scope.enterpriseId())));
        Map<Long, EnergyLine> lineMap = mapById(lineMapper.selectList(new LambdaQueryWrapper<EnergyLine>()
                .eq(EnergyLine::getEnterpriseId, scope.enterpriseId())));
        Map<Long, EnergyDevice> deviceMap = mapById(deviceMapper.selectList(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getEnterpriseId, scope.enterpriseId())));

        Set<String> nodes = new HashSet<>();
        graph.getNodes().add(new SankeyGraphVO.Node(scope.name(), 0));
        nodes.add(scope.name());

        for (Map.Entry<Long, BigDecimal> entry : workshops.entrySet()) {
            EnergyWorkshop workshop = workshopMap.get(entry.getKey());
            if (workshop == null || entry.getValue().signum() <= 0) {
                continue;
            }
            addNode(graph, nodes, workshop.getName(), 1);
            addLink(graph, scope.name(), workshop.getName(), entry.getValue());
        }

        for (Map.Entry<Long, BigDecimal> entry : lines.entrySet()) {
            EnergyLine line = lineMap.get(entry.getKey());
            if (line == null || entry.getValue().signum() <= 0) {
                continue;
            }
            EnergyWorkshop workshop = line.getWorkshopId() == null ? null : workshopMap.get(line.getWorkshopId());
            if (workshop == null) {
                continue;
            }
            addNode(graph, nodes, line.getName(), 2);
            addLink(graph, workshop.getName(), line.getName(), entry.getValue());
        }

        devices.entrySet().stream()
                .filter(e -> e.getValue().signum() > 0)
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(12)
                .forEach(entry -> {
                    EnergyDevice device = deviceMap.get(entry.getKey());
                    if (device == null || device.getLineId() == null) {
                        return;
                    }
                    EnergyLine line = lineMap.get(device.getLineId());
                    if (line == null) {
                        return;
                    }
                    addNode(graph, nodes, device.getName(), 3);
                    addLink(graph, line.getName(), device.getName(), entry.getValue());
                });
        return graph;
    }

    private Map<Long, BigDecimal> groupEnergy(int dimType, LocalDateTime start, LocalDateTime end) {
        QueryWrapper<EnergyAggregate> wrapper = new QueryWrapper<>();
        wrapper.select("dim_id", "SUM(energy_value) AS energy_value")
                .eq("deleted", 0)
                .eq("dim_type", dimType)
                .eq("period_type", PERIOD_DAY)
                .ge("period_start", start)
                .lt("period_start", end)
                .groupBy("dim_id");
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (Map<String, Object> row : aggregateMapper.selectMaps(wrapper)) {
            Long id = toLong(row.get("dim_id"));
            if (id != null) {
                result.put(id, toDecimal(row.get("energy_value")));
            }
        }
        return result;
    }

    private void addNode(SankeyGraphVO graph, Set<String> nodes, String name, int depth) {
        if (nodes.add(name)) {
            graph.getNodes().add(new SankeyGraphVO.Node(name, depth));
        }
    }

    private void addLink(SankeyGraphVO graph, String source, String target, BigDecimal value) {
        SankeyGraphVO.Link link = new SankeyGraphVO.Link();
        link.setSource(source);
        link.setTarget(target);
        link.setValue(scale2(value));
        graph.getLinks().add(link);
    }

    private List<EnergySliceVO> slices(Map<Long, BigDecimal> byType, Map<Long, EnergyType> types) {
        List<EnergySliceVO> result = new ArrayList<>();
        byType.forEach((typeId, value) -> result.add(slice(types.get(typeId), value, null)));
        result.sort((a, b) -> b.getEnergyValue().compareTo(a.getEnergyValue()));
        return result;
    }

    private EnergySliceVO slice(EnergyType type, BigDecimal energy, BigDecimal cost) {
        EnergySliceVO slice = new EnergySliceVO();
        slice.setEnergyTypeId(type == null ? null : type.getId());
        slice.setEnergyTypeName(type == null ? "未知" : type.getName());
        slice.setUnit(type == null ? null : type.getUnit());
        slice.setEnergyValue(scale2(energy));
        slice.setCostAmount(cost == null ? null : scale2(cost));
        return slice;
    }

    private List<ProfilePointVO> series(LocalDate start, LocalDate end,
                                        Map<LocalDate, BigDecimal> daily, boolean hourly) {
        List<ProfilePointVO> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            BigDecimal value = daily.getOrDefault(day, BigDecimal.ZERO);
            ProfilePointVO point = new ProfilePointVO();
            point.setTime(day.atStartOfDay());
            point.setLabel(day.toString());
            point.setEnergyValue(scale2(value));
            points.add(point);
        }
        return points;
    }

    private RatingVO rating(BigDecimal completeness, Map<LocalDate, BigDecimal> daily) {
        RatingVO rating = new RatingVO();
        List<BigDecimal> values = new ArrayList<>(daily.values());
        double stability = 100.0;
        if (values.size() > 1) {
            double mean = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            double variance = values.stream().mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2)).average().orElse(0);
            double cv = mean <= 0 ? 0 : Math.sqrt(variance) / mean;
            stability = Math.max(0, 100 - cv * 100);
        }
        int score = (int) Math.round(completeness.doubleValue() * 0.6 + stability * 0.4);
        rating.setScore(Math.max(0, Math.min(100, score)));
        rating.setGrade(score >= 90 ? "A" : score >= 75 ? "B" : score >= 60 ? "C" : "D");
        rating.setBasis("数据完整率 60% + 日均能耗稳定性 40%");
        rating.getDimensions().add("数据完整率 " + completeness + "%");
        rating.getDimensions().add("能耗稳定性 " + BigDecimal.valueOf(stability).setScale(2, RoundingMode.HALF_UP) + "分");
        return rating;
    }

    private Map<String, Object> productionSum(String column, Long id, LocalDate start, LocalDate end) {
        QueryWrapper<ProductionRecord> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(SUM(output_qty),0) AS output_qty",
                        "IFNULL(SUM(run_minutes),0) AS run_minutes",
                        "IFNULL(AVG(load_rate),0) AS load_rate")
                .eq("deleted", 0)
                .eq(column, id)
                .ge("record_date", start)
                .le("record_date", end);
        List<Map<String, Object>> rows = productionMapper.selectMaps(wrapper);
        return rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
    }

    private com.smarten.module.datalake.dto.CompletenessQueryDTO completenessQuery(Long enterpriseId, LocalDate day) {
        var query = new com.smarten.module.datalake.dto.CompletenessQueryDTO();
        query.setEnterpriseId(enterpriseId);
        query.setDate(day);
        return query;
    }

    private KpiCardVO kpi(String key, String label, BigDecimal value, String unit, boolean available, String hint) {
        KpiCardVO card = new KpiCardVO();
        card.setKey(key);
        card.setLabel(label);
        card.setValue(value == null ? null : value.setScale(2, RoundingMode.HALF_UP));
        card.setUnit(unit);
        card.setAvailable(available);
        card.setHint(hint);
        return card;
    }

    private Map<Long, String> lineNames(List<Long> ids) {
        List<Long> filtered = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (filtered.isEmpty()) {
            return Collections.emptyMap();
        }
        return lineMapper.selectBatchIds(filtered).stream()
                .collect(Collectors.toMap(EnergyLine::getId, EnergyLine::getName, (a, b) -> a));
    }

    private <T> Map<Long, T> mapById(List<T> list) {
        Map<Long, T> map = new HashMap<>();
        for (T item : list) {
            if (item instanceof EnergyWorkshop w) {
                map.put(w.getId(), item);
            } else if (item instanceof EnergyLine l) {
                map.put(l.getId(), item);
            } else if (item instanceof EnergyDevice d) {
                map.put(d.getId(), item);
            }
        }
        return map;
    }

    private int normalizeDays(Integer days) {
        return days == null || days <= 0 ? 30 : Math.min(days, 365);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale4(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
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
}
