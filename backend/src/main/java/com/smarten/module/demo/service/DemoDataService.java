package com.smarten.module.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.baseline.dto.BaselinePredictRequest;
import com.smarten.module.baseline.dto.BaselineTrainRequest;
import com.smarten.module.baseline.entity.EnergyBaseline;
import com.smarten.module.baseline.mapper.EnergyBaselineMapper;
import com.smarten.module.baseline.service.BaselineService;
import com.smarten.module.datalake.dto.IngestBatchDTO;
import com.smarten.module.datalake.dto.ReadingDTO;
import com.smarten.module.datalake.entity.EnergyRawData;
import com.smarten.module.datalake.mapper.EnergyRawDataMapper;
import com.smarten.module.datalake.service.DatalakeAggregateService;
import com.smarten.module.datalake.service.DatalakeCleanService;
import com.smarten.module.datalake.service.DatalakeGapService;
import com.smarten.module.datalake.service.DatalakeIngestService;
import com.smarten.module.datalake.service.DatalakeSimulator;
import com.smarten.module.diagnosis.dto.DiagnosisScanRequest;
import com.smarten.module.diagnosis.entity.EnergyAnomaly;
import com.smarten.module.diagnosis.mapper.EnergyAnomalyMapper;
import com.smarten.module.diagnosis.service.DiagnosisService;
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
import com.smarten.module.rectification.dto.FeedbackRequest;
import com.smarten.module.rectification.dto.TaskAssignRequest;
import com.smarten.module.rectification.dto.TaskAuditRequest;
import com.smarten.module.rectification.dto.TaskCreateRequest;
import com.smarten.module.rectification.entity.RectificationTask;
import com.smarten.module.rectification.mapper.RectificationTaskMapper;
import com.smarten.module.rectification.service.RectificationService;
import com.smarten.module.saving.dto.SavingCalculateRequest;
import com.smarten.module.saving.service.SavingService;
import com.smarten.module.saving.vo.SavingResultVO;
import com.smarten.module.system.entity.SysRole;
import com.smarten.module.system.entity.SysUser;
import com.smarten.module.system.entity.SysUserRole;
import com.smarten.module.system.mapper.SysRoleMapper;
import com.smarten.module.system.mapper.SysUserMapper;
import com.smarten.module.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 全链路演示数据生成器（幂等）。
 *
 * <p>所有采集数据 source=SIMULATOR，企业 remark 标记 DEMO；重复执行不会重复建组织或重复生成已有批次。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataService {

    private static final String ENTERPRISE_CODE = "DEMO-FULL";
    private static final String TITLE_PREFIX = "联调-";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final String[] DEVICE_TYPES = {
            "注塑机", "数控机床", "空压机", "冷却泵", "工业风机", "输送设备", "机械手", "涂覆机"
    };
    private static final double[] RATED_POWER = {45, 30, 55, 22, 18, 8, 12, 25};

    private final EnergyEnterpriseMapper enterpriseMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyMeterMapper meterMapper;
    private final EnergyTypeMapper energyTypeMapper;
    private final EnergyRawDataMapper rawMapper;
    private final EnergyBaselineMapper baselineMapper;
    private final EnergyAnomalyMapper anomalyMapper;
    private final RectificationTaskMapper taskMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final DatalakeIngestService ingestService;
    private final DatalakeCleanService cleanService;
    private final DatalakeAggregateService aggregateService;
    private final DatalakeGapService gapService;
    private final DatalakeSimulator simulator;
    private final BaselineService baselineService;
    private final DiagnosisService diagnosisService;
    private final RectificationService rectificationService;
    private final SavingService savingService;
    private final JdbcTemplate jdbc;

    /* ------------------------------ 主流程 ------------------------------ */

    public Map<String, Object> run(int days, int trainDays) {
        boolean previous = true;
        simulator.setEnabled(false);
        try {
            return runInternal(days, trainDays);
        } finally {
            simulator.setEnabled(previous);
        }
    }

    private Map<String, Object> runInternal(int days, int trainDays) {
        int totalDays = Math.max(10, Math.min(days, 60));
        int trainingDays = Math.max(2, Math.min(trainDays, totalDays - 7));
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(totalDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long begin = System.currentTimeMillis();
        Context context = ensureOrganization();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enterpriseId", context.enterprise().getId());
        result.put("enterpriseName", context.enterprise().getName());
        result.put("workshops", context.workshops().size());
        result.put("lines", context.lines().size());
        result.put("devices", context.devices().size());
        result.put("meters", context.meters().size());

        int readings = generateSeries(context, start, end, trainingDays);
        result.put("readings", readings);

        aggregateService.aggregateRange(start, end, java.util.List.of(1, 2, 3));
        result.put("details", count("SELECT COUNT(*) FROM energy_detail WHERE enterprise_id = ? AND period_start >= ? AND period_start < ?",
                context.enterprise().getId(), start, end));
        result.put("aggregates", count("SELECT COUNT(*) FROM energy_aggregate WHERE enterprise_id = ? AND period_start >= ? AND period_start < ?",
                context.enterprise().getId(), start, end));

        int models = trainBaselines(context, start, trainingDays);
        result.put("baselines", models);

        LocalDateTime observationStart = end.minusDays(7);
        predictAll(context, start.plusDays(trainingDays), end);
        int anomalies = diagnose(context, start.plusDays(trainingDays), end);
        result.put("anomalies", anomalies);

        int tasks = simulateRectification(context, anomalies);
        result.put("tasks", tasks);

        predictForDevice(context.devices().get(0), observationStart, end);
        SavingResultVO saving = simulateSaving(context, observationStart);
        if (saving != null) {
            result.put("savingValue", saving.getSavingValue());
            result.put("savingRate", saving.getSavingRate());
            result.put("savingConclusion", saving.getConclusion());
        }
        result.put("gapCount", gapService.detect(end.toLocalDate().minusDays(1), context.enterprise().getId()));
        result.put("elapsedMs", System.currentTimeMillis() - begin);
        result.put("source", "SIMULATOR");
        result.put("marked", "企业备注与生产/采集数据均标记模拟来源");
        return result;
    }

    public Map<String, Object> reset() {
        simulator.setEnabled(false);
        EnergyEnterprise enterprise = enterpriseMapper.selectOne(new LambdaQueryWrapper<EnergyEnterprise>()
                .eq(EnergyEnterprise::getCode, ENTERPRISE_CODE).last("LIMIT 1"));
        Map<String, Object> result = new LinkedHashMap<>();
        if (enterprise == null) {
            result.put("reset", false);
            return result;
        }
        Long id = enterprise.getId();
        for (String sql : List.of(
                "DELETE FROM energy_saving_result WHERE enterprise_id = ?",
                "DELETE FROM diagnosis_report WHERE enterprise_id = ?",
                "DELETE FROM energy_anomaly WHERE enterprise_id = ?",
                "DELETE FROM rectification_feedback WHERE task_id IN (SELECT id FROM rectification_task WHERE enterprise_id = ?)",
                "DELETE FROM rectification_task WHERE enterprise_id = ?",
                "DELETE FROM energy_baseline_detail WHERE enterprise_id = ?",
                "DELETE FROM energy_baseline WHERE enterprise_id = ?",
                "DELETE FROM production_record WHERE enterprise_id = ?",
                "DELETE FROM energy_detail WHERE enterprise_id = ?",
                "DELETE FROM energy_aggregate WHERE enterprise_id = ?",
                "DELETE FROM energy_raw_data WHERE enterprise_id = ?",
                "DELETE FROM energy_data_gap WHERE enterprise_id = ?")) {
            jdbc.update(sql, id);
        }
        jdbc.update("DELETE FROM data_ingestion_log WHERE batch_no LIKE 'DEMO-%'");
        jdbc.update("DELETE FROM data_ingest_failure WHERE batch_no LIKE 'DEMO-%'");
        result.put("reset", true);
        result.put("enterpriseId", id);
        return result;
    }

    public Map<String, Object> status() {
        EnergyEnterprise enterprise = enterpriseMapper.selectOne(new LambdaQueryWrapper<EnergyEnterprise>()
                .eq(EnergyEnterprise::getCode, ENTERPRISE_CODE).last("LIMIT 1"));
        Map<String, Object> map = new LinkedHashMap<>();
        if (enterprise == null) {
            map.put("exists", false);
            return map;
        }
        Long enterpriseId = enterprise.getId();
        map.put("exists", true);
        map.put("enterpriseId", enterpriseId);
        map.put("workshops", count("SELECT COUNT(*) FROM energy_workshop WHERE enterprise_id = ?", enterpriseId));
        map.put("lines", count("SELECT COUNT(*) FROM energy_line WHERE enterprise_id = ?", enterpriseId));
        map.put("devices", count("SELECT COUNT(*) FROM energy_device WHERE enterprise_id = ?", enterpriseId));
        map.put("meters", count("SELECT COUNT(*) FROM energy_meter WHERE enterprise_id = ?", enterpriseId));
        map.put("raw", count("SELECT COUNT(*) FROM energy_raw_data WHERE enterprise_id = ?", enterpriseId));
        map.put("production", count("SELECT COUNT(*) FROM production_record WHERE enterprise_id = ?", enterpriseId));
        map.put("anomalies", count("SELECT COUNT(*) FROM energy_anomaly WHERE enterprise_id = ?", enterpriseId));
        map.put("tasks", count("SELECT COUNT(*) FROM rectification_task WHERE enterprise_id = ?", enterpriseId));
        map.put("savingResults", count("SELECT COUNT(*) FROM energy_saving_result WHERE enterprise_id = ?", enterpriseId));
        return map;
    }

    /* ------------------------------ 组织 ------------------------------ */

    private Context ensureOrganization() {
        EnergyEnterprise enterprise = enterpriseMapper.selectOne(new LambdaQueryWrapper<EnergyEnterprise>()
                .eq(EnergyEnterprise::getCode, ENTERPRISE_CODE).last("LIMIT 1"));
        if (enterprise == null) {
            enterprise = new EnergyEnterprise();
            enterprise.setCode(ENTERPRISE_CODE);
            enterprise.setName("联调示范制造有限公司");
            enterprise.setIndustry("汽车零部件");
            enterprise.setRegion("江苏省苏州市");
            enterprise.setAddress("工业园区联调路 1 号");
            enterprise.setContactName("王工");
            enterprise.setContactPhone("13800000000");
            enterprise.setStatus(1);
            enterprise.setRemark("DEMO 全链路联调数据；采集 source=SIMULATOR，禁止作为真实数据使用");
            enterpriseMapper.insert(enterprise);
        }

        Map<String, EnergyType> types = new LinkedHashMap<>();
        energyTypeMapper.selectList(null).forEach(type -> types.put(type.getCode(), type));

        List<EnergyWorkshop> workshops = new ArrayList<>();
        String[] workshopNames = {"冲压车间", "注塑车间", "装配车间"};
        for (int i = 0; i < 3; i++) {
            String code = "DEMO-WS" + (i + 1);
            EnergyWorkshop workshop = workshopMapper.selectOne(new LambdaQueryWrapper<EnergyWorkshop>()
                    .eq(EnergyWorkshop::getCode, code).last("LIMIT 1"));
            if (workshop == null) {
                workshop = new EnergyWorkshop();
                workshop.setEnterpriseId(enterprise.getId());
                workshop.setCode(code);
                workshop.setName(workshopNames[i]);
                workshop.setManager("车间主任" + (i + 1));
                workshop.setArea(BigDecimal.valueOf(2400 + i * 400));
                workshop.setStatus(1);
                workshop.setRemark("DEMO");
                workshopMapper.insert(workshop);
            }
            workshops.add(workshop);
        }

        List<EnergyLine> lines = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String code = "DEMO-LN" + (i + 1);
            EnergyLine line = lineMapper.selectOne(new LambdaQueryWrapper<EnergyLine>()
                    .eq(EnergyLine::getCode, code).last("LIMIT 1"));
            if (line == null) {
                line = new EnergyLine();
                line.setEnterpriseId(enterprise.getId());
                line.setWorkshopId(workshops.get(i / 2).getId());
                line.setCode(code);
                line.setName(workshopNames[i / 2].replace("车间", "") + (i % 2 + 1) + "线");
                line.setProductType(i % 2 == 0 ? "A型壳体" : "B型支架");
                line.setDesignCapacity(BigDecimal.valueOf(1200 + i * 150));
                line.setCapacityUnit("件/天");
                line.setStatus(1);
                line.setRemark("DEMO");
                lineMapper.insert(line);
            }
            lines.add(line);
        }

        List<EnergyDevice> devices = new ArrayList<>();
        List<MeterInfo> meters = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String code = "DEMO-DEV" + String.format("%02d", i + 1);
            EnergyDevice device = deviceMapper.selectOne(new LambdaQueryWrapper<EnergyDevice>()
                    .eq(EnergyDevice::getCode, code).last("LIMIT 1"));
            int typeIndex = i % DEVICE_TYPES.length;
            String energyCode = switch (i % 10) {
                case 6 -> "WATER";
                case 7 -> "GAS";
                case 8 -> "STEAM";
                default -> "ELEC";
            };
            EnergyType type = types.get(energyCode);
            if (device == null) {
                device = new EnergyDevice();
                device.setEnterpriseId(enterprise.getId());
                device.setWorkshopId(workshops.get(i / 10).getId());
                device.setLineId(lines.get(i / 5).getId());
                device.setCode(code);
                device.setName(DEVICE_TYPES[typeIndex] + " " + (char) ('A' + (i / 10)) + (i % 10 + 1));
                device.setDeviceType(DEVICE_TYPES[typeIndex]);
                device.setModel("M-" + (100 + i));
                device.setRatedPower(BigDecimal.valueOf(RATED_POWER[typeIndex]));
                device.setEnergyTypeId(type == null ? null : type.getId());
                device.setCommissionDate(LocalDate.now().minusYears(2).plusDays(i * 7L));
                device.setStatus(i == 17 ? 2 : 1);
                device.setRemark("DEMO");
                deviceMapper.insert(device);
            }
            devices.add(device);

            EnergyMeter meter = meterMapper.selectOne(new LambdaQueryWrapper<EnergyMeter>()
                    .eq(EnergyMeter::getMeterCode, "DEMO-M-" + code).last("LIMIT 1"));
            if (meter == null) {
                meter = new EnergyMeter();
                meter.setEnterpriseId(enterprise.getId());
                meter.setDeviceId(device.getId());
                meter.setMeterCode("DEMO-M-" + code);
                meter.setMeterName(device.getName() + " 计量");
                meter.setEnergyTypeId(type == null ? null : type.getId());
                meter.setRatio(BigDecimal.ONE);
                meter.setCollectIntervalSeconds(3600);
                meter.setReadingType(1);
                meter.setInstallTime(LocalDateTime.now().minusYears(2));
                meter.setStatus(1);
                meter.setRemark("DEMO");
                meterMapper.insert(meter);
            }
            meters.add(new MeterInfo(meter, "DEVICE", device.getId(), lines.get(i / 5).getId(),
                    workshops.get(i / 10).getId(), type));
        }

        for (int i = 0; i < 6; i++) {
            EnergyMeter meter = ensureAggregateMeter(enterprise, types.get("ELEC"), "DEMO-M-LN" + (i + 1),
                    "产线总表" + (i + 1), "LINE", lines.get(i).getId(), lines.get(i).getWorkshopId());
            meters.add(new MeterInfo(meter, "LINE", lines.get(i).getId(), lines.get(i).getId(),
                    lines.get(i).getWorkshopId(), types.get("ELEC")));
        }
        for (int i = 0; i < 3; i++) {
            EnergyMeter meter = ensureAggregateMeter(enterprise, types.get("ELEC"), "DEMO-M-WS" + (i + 1),
                    "车间总表" + (i + 1), "WORKSHOP", workshops.get(i).getId(), workshops.get(i).getId());
            meters.add(new MeterInfo(meter, "WORKSHOP", workshops.get(i).getId(), null,
                    workshops.get(i).getId(), types.get("ELEC")));
        }
        EnergyMeter enterpriseMeter = ensureAggregateMeter(enterprise, types.get("ELEC"), "DEMO-M-ENT",
                "企业总表", "ENTERPRISE", enterprise.getId(), null);
        meters.add(new MeterInfo(enterpriseMeter, "ENTERPRISE", enterprise.getId(), null, null, types.get("ELEC")));

        ensureDemoEngineer(enterprise);
        return new Context(enterprise, workshops, lines, devices, meters);
    }

    private void ensureDemoEngineer(EnergyEnterprise enterprise) {
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "demo_engineer").last("LIMIT 1"));
        if (exists != null) {
            return;
        }
        SysUser template = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "engineer").last("LIMIT 1"));
        if (template == null) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername("demo_engineer");
        user.setPassword(template.getPassword());
        user.setRealName("演示工程师");
        user.setPhone("13900000000");
        user.setEnterpriseId(enterprise.getId());
        user.setStatus(1);
        user.setRemark("DEMO 联调账号，密码同 engineer");
        userMapper.insert(user);
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "ENGINEER").last("LIMIT 1"));
        if (role != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
    }

    private EnergyMeter ensureAggregateMeter(EnergyEnterprise enterprise, EnergyType type, String code,
                                             String name, String kind, Long refId, Long workshopId) {
        EnergyMeter meter = meterMapper.selectOne(new LambdaQueryWrapper<EnergyMeter>()
                .eq(EnergyMeter::getMeterCode, code).last("LIMIT 1"));
        if (meter != null) {
            return meter;
        }
        meter = new EnergyMeter();
        meter.setEnterpriseId(enterprise.getId());
        meter.setMeterCode(code);
        meter.setMeterName(name);
        meter.setEnergyTypeId(type == null ? null : type.getId());
        meter.setRatio(BigDecimal.ONE);
        meter.setCollectIntervalSeconds(3600);
        meter.setReadingType(1);
        meter.setInstallTime(LocalDateTime.now().minusYears(2));
        meter.setStatus(1);
        meter.setRemark("DEMO " + kind);
        if ("LINE".equals(kind)) {
            meter.setLineId(refId);
        }
        meterMapper.insert(meter);
        return meter;
    }

    /* ------------------------------ 数据生成 ------------------------------ */

    private int generateSeries(Context context, LocalDateTime start, LocalDateTime end, int trainingDays) {
        Map<Long, BigDecimal> bases = new HashMap<>();
        for (MeterInfo info : context.meters()) {
            EnergyRawData last = rawMapper.selectOne(new LambdaQueryWrapper<EnergyRawData>()
                    .eq(EnergyRawData::getMeterId, info.meter().getId())
                    .lt(EnergyRawData::getCollectTime, start)
                    .orderByDesc(EnergyRawData::getCollectTime)
                    .last("LIMIT 1"));
            bases.put(info.meter().getId(), last == null
                    ? BigDecimal.valueOf(1000 + info.meter().getId() * 7L) : last.getRawValue());
        }

        int totalDays = (int) ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        Map<Integer, Injection> injections = injections(totalDays, trainingDays);
        Map<Integer, Double> savingFactor = new HashMap<>();
        savingFactor.put(0, 0.85);
        LocalDateTime savingFrom = start.plusDays(trainingDays);

        int inserted = 0;
        List<Object[]> productionRows = new ArrayList<>();
        for (LocalDateTime hour = start; hour.isBefore(end); hour = hour.plusHours(1)) {
            int dayIndex = (int) ChronoUnit.DAYS.between(start.toLocalDate(), hour.toLocalDate());
            String batchNo = "DEMO-" + context.enterprise().getId() + "-" + hour.format(STAMP);
            List<ReadingDTO> readings = new ArrayList<>();
            Map<Long, Double> lineEnergy = new HashMap<>();
            Map<Long, Double> workshopEnergy = new HashMap<>();

            for (int index = 0; index < context.devices().size(); index++) {
                EnergyDevice device = context.devices().get(index);
                MeterInfo meterInfo = context.deviceMeters().get(index);
                Injection injection = injections.get(index);
                boolean injected = injection != null && injection.matches(dayIndex, hour.getHour());
                if (injected && injection.type().equals("MISSING")) {
                    continue;
                }

                Condition condition = conditionOf(device, index, hour, dayIndex, trainingDays,
                        savingFrom, savingFactor, injection, injected, meterInfo.energyType());
                EnergyMeter meter = meterInfo.meter();
                BigDecimal base = bases.getOrDefault(meter.getId(), BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
                readings.add(reading(meter.getId(), hour, base));
                bases.put(meter.getId(), base.add(BigDecimal.valueOf(condition.energy())).setScale(3, RoundingMode.HALF_UP));
                inserted++;

                if (condition.energyType() != null && "ELEC".equals(condition.energyType().getCode())) {
                    lineEnergy.merge(device.getLineId(), condition.energy(), Double::sum);
                    workshopEnergy.merge(device.getWorkshopId(), condition.energy(), Double::sum);
                }
                productionRows.add(productionRow(context.enterprise(), device, hour, condition, dayIndex));
            }

            for (MeterInfo info : context.meters()) {
                if ("DEVICE".equals(info.kind())) {
                    continue;
                }
                double energy = switch (info.kind()) {
                    case "LINE" -> lineEnergy.getOrDefault(info.refId(), 0.0) * 1.02;
                    case "WORKSHOP" -> workshopEnergy.getOrDefault(info.refId(), 0.0) * 1.01;
                    default -> workshopEnergy.values().stream().mapToDouble(Double::doubleValue).sum() * 1.01;
                };
                BigDecimal base = bases.getOrDefault(info.meter().getId(), BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
                readings.add(reading(info.meter().getId(), hour, base));
                bases.put(info.meter().getId(), base.add(BigDecimal.valueOf(energy)).setScale(3, RoundingMode.HALF_UP));
                inserted++;
            }

            if (!readings.isEmpty()) {
                IngestBatchDTO batch = new IngestBatchDTO();
                batch.setBatchNo(batchNo);
                batch.setSource("SIMULATOR");
                batch.setReadings(readings);
                ingestService.ingestSystem(batch);
                cleanService.cleanBatch(batchNo);
            }

            if (hour.getHour() == 10 && dayIndex == Math.max(0, trainingDays + 3)) {
                duplicate(context, hour, bases);
            }
        }

        saveProduction(productionRows);
        return inserted;
    }

    private void duplicate(Context context, LocalDateTime hour, Map<Long, BigDecimal> bases) {
        MeterInfo info = context.deviceMeters().get(20);
        IngestBatchDTO batch = new IngestBatchDTO();
        batch.setBatchNo("DEMO-DUP-" + context.enterprise().getId() + "-" + hour.format(STAMP));
        batch.setSource("SIMULATOR");
        ReadingDTO reading = new ReadingDTO();
        reading.setMeterId(info.meter().getId());
        reading.setCollectTime(hour);
        reading.setValue(bases.getOrDefault(info.meter().getId(), BigDecimal.TEN));
        reading.setReadingType(1);
        batch.setReadings(List.of(reading));
        ingestService.ingestSystem(batch);
    }

    private Condition conditionOf(EnergyDevice device, int index, LocalDateTime hour, int dayIndex,
                                  int trainingDays, LocalDateTime savingFrom, Map<Integer, Double> savingFactor,
                                  Injection injection, boolean injected, EnergyType energyType) {
        int h = hour.getHour();
        boolean standby = h >= 22 || h < 6;
        int status = standby ? 2 : 1;
        int runMinutes = standby ? 0 : 60;
        double rated = device.getRatedPower() == null ? 20 : device.getRatedPower().doubleValue();
        Random random = new Random(device.getId() * 1000L + hour.toLocalDate().toEpochDay() * 24 + h);
        double load;
        double energy;
        if (standby) {
            load = 0;
            energy = rated * 0.06 * (0.9 + random.nextDouble() * 0.2);
        } else {
            load = 45 + 25 * Math.sin((h - 6) / 16.0 * Math.PI) + random.nextDouble() * 8;
            energy = rated * (load / 100.0) * (0.9 + random.nextDouble() * 0.2);
        }
        String product = dayIndex % 2 == 0 ? "A型壳体" : "B型支架";

        if (index == 0 && !hour.isBefore(savingFrom)) {
            energy *= savingFactor.getOrDefault(0, 0.85);
        }

        if (injected && injection != null) {
            switch (injection.type()) {
                case "SURGE", "SUSTAINED", "NIGHT", "UNIT" -> {
                    energy *= injection.multiplier();
                    load = Math.min(98, load * 1.25 + 10);
                    status = 1;
                    runMinutes = 60;
                    if ("UNIT".equals(injection.type())) {
                        load = 85;
                    }
                }
                case "NO_LOAD" -> {
                    energy *= injection.multiplier();
                    load = 0;
                    status = 1;
                    runMinutes = 60;
                }
                case "IDLE" -> {
                    energy = 1.5;
                    load = 0;
                    status = 2;
                    runMinutes = 0;
                }
                default -> {
                }
            }
        }

        double output = (status == 1 && load > 0)
                ? energy * outputFactor(device.getDeviceType()) * (0.9 + random.nextDouble() * 0.2)
                : 0;
        if (injected && injection != null && "UNIT".equals(injection.type())) {
            output = 2;
        }
        double ambient = 18 + 6 * Math.sin((hour.getMonthValue() - 1) / 12.0 * 2 * Math.PI)
                + 3 * Math.sin(h / 24.0 * 2 * Math.PI);
        return new Condition(Math.max(0, energy), Math.min(100, load), output, runMinutes, status,
                ambient, product, energyType);
    }

    private Object[] productionRow(EnergyEnterprise enterprise, EnergyDevice device, LocalDateTime hour,
                                   Condition condition, int dayIndex) {
        int shift = hour.getHour() < 8 ? 3 : hour.getHour() < 16 ? 1 : 2;
        return new Object[]{
                enterprise.getId(), device.getWorkshopId(), device.getLineId(), device.getId(),
                hour.toLocalDate(), shift, hour, hour.plusHours(1), condition.product(),
                BigDecimal.valueOf(condition.output()).setScale(2, RoundingMode.HALF_UP),
                "件", condition.runMinutes(), BigDecimal.valueOf(condition.load()),
                BigDecimal.valueOf(condition.ambient()), condition.status(), "SIMULATOR"
        };
    }

    private void saveProduction(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO production_record (enterprise_id, workshop_id, line_id, device_id, record_date, shift,
                                               start_time, end_time, product_type, output_qty, output_unit,
                                               run_minutes, load_rate, ambient_temp, status, data_source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE output_qty = VALUES(output_qty), load_rate = VALUES(load_rate),
                                        run_minutes = VALUES(run_minutes), status = VALUES(status),
                                        product_type = VALUES(product_type), ambient_temp = VALUES(ambient_temp)
                """;
        jdbc.batchUpdate(sql, rows);
    }

    /* ------------------------------ 基线 / 诊断 / 整改 / 核算 ------------------------------ */

    private int trainBaselines(Context context, LocalDateTime start, int trainingDays) {
        int trained = 0;
        LocalDate from = start.toLocalDate();
        LocalDate to = start.plusDays(trainingDays - 1L).toLocalDate();
        for (int i = 0; i < context.devices().size(); i++) {
            EnergyDevice device = context.devices().get(i);
            Long active = baselineMapper.selectCount(new LambdaQueryWrapper<EnergyBaseline>()
                    .eq(EnergyBaseline::getDimType, 4)
                    .eq(EnergyBaseline::getDimId, device.getId())
                    .eq(EnergyBaseline::getStatus, 1));
            if (active != null && active > 0) {
                continue;
            }
            BaselineTrainRequest request = new BaselineTrainRequest();
            request.setDimType(4);
            request.setDimId(device.getId());
            request.setEnergyTypeId(device.getEnergyTypeId());
            request.setStartDate(from);
            request.setEndDate(to);
            request.setActivate(true);
            try {
                baselineService.train(request);
                trained++;
            } catch (Exception exception) {
                log.warn("基线训练跳过 device={} reason={}", device.getId(), exception.getMessage());
            }
        }
        return trained;
    }

    private void predictForDevice(EnergyDevice device, LocalDateTime start, LocalDateTime end) {
        BaselinePredictRequest request = new BaselinePredictRequest();
        request.setDimType(4);
        request.setDimId(device.getId());
        request.setEnergyTypeId(device.getEnergyTypeId());
        request.setStartDate(start.toLocalDate());
        request.setEndDate(end.toLocalDate());
        try {
            baselineService.predict(request);
        } catch (Exception exception) {
            log.warn("基线预测失败 device={}: {}", device.getId(), exception.getMessage());
        }
    }

    private void predictAll(Context context, LocalDateTime start, LocalDateTime end) {
        for (EnergyDevice device : context.devices()) {
            predictForDevice(device, start, end);
        }
    }

    private int diagnose(Context context, LocalDateTime start, LocalDateTime end) {
        DiagnosisScanRequest request = new DiagnosisScanRequest();
        request.setEnterpriseId(context.enterprise().getId());
        request.setStart(start);
        request.setEnd(end);
        try {
            var scan = diagnosisService.scan(request);
            return scan.getNewAnomalies() + scan.getMergedAnomalies();
        } catch (Exception exception) {
            log.warn("诊断扫描失败: {}", exception.getMessage());
            return 0;
        }
    }

    private int simulateRectification(Context context, int anomalyCount) {
        Long engineerId = engineerId(context.enterprise().getId());
        List<EnergyAnomaly> anomalies = anomalyMapper.selectList(new LambdaQueryWrapper<EnergyAnomaly>()
                .eq(EnergyAnomaly::getEnterpriseId, context.enterprise().getId())
                .isNotNull(EnergyAnomaly::getReportId)
                .orderByDesc(EnergyAnomaly::getSeverity)
                .last("LIMIT 2"));

        int created = 0;
        for (EnergyAnomaly anomaly : anomalies) {
            Long exists = taskMapper.selectCount(new LambdaQueryWrapper<RectificationTask>()
                    .eq(RectificationTask::getAnomalyId, anomaly.getId()));
            if (exists != null && exists > 0) {
                continue;
            }
            TaskCreateRequest request = new TaskCreateRequest();
            request.setReportId(anomaly.getReportId());
            request.setTitle(TITLE_PREFIX + anomaly.getRuleCode() + " 整改");
            request.setMeasure("复核工况并优化运行参数");
            request.setExpectedSaving(BigDecimal.valueOf(120));
            request.setPriority(3);
            request.setAssigneeId(engineerId);
            request.setDeadline(LocalDateTime.now().plusDays(5));
            var task = rectificationService.create(request);
            simulateFlow(task.getId(), engineerId);
            created++;
        }
        return created;
    }

    private SavingResultVO simulateSaving(Context context, LocalDateTime observationStart) {
        EnergyDevice device = context.devices().get(0);
        Long engineerId = engineerId(context.enterprise().getId());
        RectificationTask exists = taskMapper.selectOne(new LambdaQueryWrapper<RectificationTask>()
                .eq(RectificationTask::getEnterpriseId, context.enterprise().getId())
                .eq(RectificationTask::getDeviceId, device.getId())
                .likeRight(RectificationTask::getTitle, TITLE_PREFIX + "节能")
                .last("LIMIT 1"));
        Long taskId;
        if (exists == null) {
            TaskCreateRequest request = new TaskCreateRequest();
            request.setDeviceId(device.getId());
            request.setTitle(TITLE_PREFIX + "节能改造");
            request.setMeasure("空压系统联动优化，降低单位产品能耗");
            request.setExpectedSaving(BigDecimal.valueOf(600));
            request.setPriority(4);
            request.setAssigneeId(engineerId);
            request.setDeadline(LocalDateTime.now().plusDays(5));
            var task = rectificationService.create(request);
            taskId = task.getId();
        } else {
            taskId = exists.getId();
        }
        RectificationTask current = taskMapper.selectById(taskId);
        if (current == null || current.getStatus() == null || current.getStatus() != 7) {
            simulateFlow(taskId, engineerId);
        }
        SavingCalculateRequest request = new SavingCalculateRequest();
        request.setObservationDays(7);
        request.setBasePrice(BigDecimal.valueOf(0.68));
        request.setStartTime(observationStart);
        try {
            return savingService.calculate(taskId, request);
        } catch (Exception exception) {
            log.warn("核算失败 task={}: {}", taskId, exception.getMessage());
            return null;
        }
    }

    private void simulateFlow(Long taskId, Long engineerId) {
        RectificationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        int status = task.getStatus() == null ? 0 : task.getStatus();
        if (status == 0) {
            TaskAssignRequest assign = new TaskAssignRequest();
            assign.setAssigneeId(engineerId);
            rectificationService.assign(taskId, assign);
        }
        try {
            rectificationService.start(taskId, engineerId);
        } catch (Exception ignored) {
            // 已开始
        }
        FeedbackRequest feedback = new FeedbackRequest();
        feedback.setContent("已完成整改并复核运行参数");
        feedback.setBeforeDesc("设备空载/夜间能耗偏高");
        feedback.setAfterDesc("运行参数优化后能耗下降");
        feedback.setActualSaving(BigDecimal.valueOf(90));
        feedback.setComplete(true);
        feedback.setImages(List.of());
        try {
            rectificationService.feedback(taskId, engineerId, feedback);
        } catch (Exception ignored) {
            // 已提交
        }
        TaskAuditRequest audit = new TaskAuditRequest();
        audit.setPass(true);
        audit.setRemark("联调自动审核通过");
        try {
            rectificationService.audit(taskId, audit);
        } catch (Exception ignored) {
            // 已审核
        }
    }

    private Long engineerId(Long enterpriseId) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEnterpriseId, enterpriseId)
                .eq(SysUser::getStatus, 1)
                .last("LIMIT 1"));
        if (user != null) {
            return user.getId();
        }
        SysUser fallback = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "engineer").last("LIMIT 1"));
        if (fallback == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少工程师账号，请先执行种子数据");
        }
        return fallback.getId();
    }

    /* ------------------------------ 辅助 ------------------------------ */

    private Map<Integer, Injection> injections(int totalDays, int trainingDays) {
        Map<Integer, Injection> map = new HashMap<>();
        int base = Math.max(trainingDays + 1, totalDays - 6);
        map.put(1, new Injection(base, 10, 2, "SURGE", 2.2));
        map.put(3, new Injection(base + 1, 8, 4, "SUSTAINED", 1.6));
        map.put(6, new Injection(base + 2, 14, 1, "NO_LOAD", 1.9));
        map.put(9, new Injection(base + 3, 2, 3, "IDLE", 1));
        map.put(12, new Injection(base + 4, 9, 1, "UNIT", 1.8));
        map.put(15, new Injection(base + 5, 23, 1, "NIGHT", 1.9));
        map.put(18, new Injection(base + 4, 0, 4, "MISSING", 1));
        return map;
    }

    private ReadingDTO reading(Long meterId, LocalDateTime time, BigDecimal value) {
        ReadingDTO reading = new ReadingDTO();
        reading.setMeterId(meterId);
        reading.setCollectTime(time);
        reading.setValue(value);
        reading.setReadingType(1);
        return reading;
    }

    private double outputFactor(String deviceType) {
        return switch (deviceType) {
            case "注塑机" -> 2.2;
            case "机械手" -> 5.0;
            case "涂覆机" -> 1.2;
            case "空压机" -> 3.0;
            default -> 2.0;
        };
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private record MeterInfo(EnergyMeter meter, String kind, Long refId, Long lineId, Long workshopId,
                             EnergyType energyType) {
    }

    private record Condition(double energy, double load, double output, int runMinutes, int status,
                             double ambient, String product, EnergyType energyType) {
    }

    private record Injection(int day, int hour, int hours, String type, double multiplier) {
        boolean matches(int dayIndex, int currentHour) {
            return dayIndex == day && currentHour >= hour && currentHour < hour + hours;
        }
    }

    private record Context(EnergyEnterprise enterprise, List<EnergyWorkshop> workshops, List<EnergyLine> lines,
                           List<EnergyDevice> devices, List<MeterInfo> meters) {
        List<MeterInfo> deviceMeters() {
            return meters.stream().filter(item -> "DEVICE".equals(item.kind())).toList();
        }
    }
}
