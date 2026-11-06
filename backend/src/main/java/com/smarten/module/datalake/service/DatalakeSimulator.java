package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smarten.module.datalake.dto.IngestBatchDTO;
import com.smarten.module.datalake.dto.ReadingDTO;
import com.smarten.module.datalake.entity.EnergyAggregate;
import com.smarten.module.datalake.entity.EnergyRawData;
import com.smarten.module.datalake.entity.ProductionRecord;
import com.smarten.module.datalake.mapper.EnergyAggregateMapper;
import com.smarten.module.datalake.mapper.EnergyRawDataMapper;
import com.smarten.module.datalake.mapper.ProductionRecordMapper;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟采集任务：每分钟为所有启用仪表生成一条读数。
 *
 * <p>能耗与工况关联：设备状态（运行/停机/检修）、班次时段、工作日/周末、随机波动。
 * 累计读数表生成「累计值」，周期增量表生成「区间增量」，两者严格区分。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatalakeSimulator {

    private static final DateTimeFormatter BATCH_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final EnergyMeterMapper meterMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyRawDataMapper rawMapper;
    private final EnergyAggregateMapper aggregateMapper;
    private final ProductionRecordMapper productionMapper;
    private final DatalakeIngestService ingestService;
    private final DatalakeCleanService cleanService;
    private final DatalakeAggregateService aggregateService;

    @Value("${smarten.simulator.enabled:true}")
    private boolean enabled;

    public synchronized void setEnabled(boolean value) {
        this.enabled = value;
    }

    @Scheduled(cron = "0 * * * * *")
    public synchronized void scheduledTick() {
        if (!enabled) {
            return;
        }
        LocalDateTime end = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime start = end.minusMinutes(1);
        try {
            int count = simulate(start, end, 1);
            log.debug("模拟采集完成: {} 条, 区间 {} ~ {}", count, start, end);
        } catch (Exception ex) {
            log.error("模拟采集失败", ex);
        }
    }

    /**
     * 生成并入库模拟数据。
     *
     * @return 生成的读数条数
     */
    public synchronized int simulate(LocalDateTime start, LocalDateTime end, int stepMinutes) {
        if (start == null || end == null || !start.isBefore(end)) {
            return 0;
        }
        int step = Math.max(1, stepMinutes);
        List<EnergyMeter> meters = meterMapper.selectList(new LambdaQueryWrapper<EnergyMeter>()
                .eq(EnergyMeter::getStatus, 1));
        if (meters.isEmpty()) {
            return 0;
        }
        Map<Long, EnergyDevice> deviceCache = new HashMap<>();
        Map<Long, BigDecimal> lastValues = new HashMap<>();
        for (EnergyMeter meter : meters) {
            if (meter.getDeviceId() != null) {
                deviceCache.computeIfAbsent(meter.getDeviceId(), id -> deviceMapper.selectById(id));
            }
            lastValues.put(meter.getId(), lastReading(meter.getId(), start));
        }

        int total = 0;
        for (LocalDateTime t = start; t.isBefore(end); t = t.plusMinutes(step)) {
            String batchNo = "SIM-" + t.format(BATCH_FORMAT);
            List<ReadingDTO> readings = new ArrayList<>(meters.size());
            for (EnergyMeter meter : meters) {
                EnergyDevice device = meter.getDeviceId() == null ? null : deviceCache.get(meter.getDeviceId());
                BigDecimal energy = energyFor(device, t, step);
                BigDecimal ratio = meter.getRatio() == null || meter.getRatio().signum() <= 0
                        ? BigDecimal.ONE : meter.getRatio();
                int readingType = meter.getReadingType() == null ? 1 : meter.getReadingType();
                BigDecimal value;
                if (readingType == 2) {
                    value = energy.divide(ratio, 4, RoundingMode.HALF_UP);
                } else {
                    BigDecimal last = lastValues.getOrDefault(meter.getId(), BigDecimal.ZERO);
                    value = last.add(energy.divide(ratio, 4, RoundingMode.HALF_UP));
                }
                lastValues.put(meter.getId(), value);

                ReadingDTO reading = new ReadingDTO();
                reading.setMeterId(meter.getId());
                reading.setCollectTime(t);
                reading.setValue(value.setScale(4, RoundingMode.HALF_UP));
                reading.setReadingType(readingType);
                reading.setPointCode("TOTAL");
                readings.add(reading);
            }

            IngestBatchDTO batch = new IngestBatchDTO();
            batch.setBatchNo(batchNo);
            batch.setSource("SIMULATOR");
            batch.setReadings(readings);
            ingestService.ingestSystem(batch);
            cleanService.cleanBatch(batchNo);
            total += readings.size();
        }

        aggregateService.aggregateRange(start, end);
        simulateProduction(start, end);
        return total;
    }

    /**
     * 按小时聚合结果生成生产工况（产量、运行时长、负荷率），供单耗与画像使用。
     * 与能耗联动：同一小时的产量由能耗换算，保证「单耗」指标可计算且稳定。
     */
    private void simulateProduction(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<EnergyAggregate> wrapper = new QueryWrapper<>();
        wrapper.select("dim_id", "period_start", "SUM(energy_value) AS energy_value")
                .eq("deleted", 0)
                .eq("dim_type", 4)
                .eq("period_type", 1)
                .ge("period_start", start)
                .lt("period_start", end)
                .groupBy("dim_id", "period_start");

        for (java.util.Map<String, Object> row : aggregateMapper.selectMaps(wrapper)) {
            Long deviceId = row.get("dim_id") == null ? null : ((Number) row.get("dim_id")).longValue();
            if (deviceId == null) {
                continue;
            }
            LocalDateTime hourStart = toDateTime(row.get("period_start"));
            if (hourStart == null) {
                continue;
            }
            BigDecimal energy = new BigDecimal(String.valueOf(row.get("energy_value")));
            EnergyDevice device = deviceMapper.selectById(deviceId);
            if (device == null) {
                continue;
            }
            int hour = hourStart.getHour();
            int shift = hour < 8 ? 3 : hour < 16 ? 1 : 2;
            boolean standby = hour >= 22 || hour < 6;
            int status = standby ? 2 : 1;
            int runMinutes = standby ? 0 : 60;
            double rated = device.getRatedPower() == null ? 30.0 : device.getRatedPower().doubleValue();
            double jitter = ThreadLocalRandom.current().nextDouble(-6, 6);
            BigDecimal loadRate = standby
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(Math.min(96, Math.max(8, energy.doubleValue() / rated * 100 * 3.2 + jitter)))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal output = standby
                    ? BigDecimal.ZERO
                    : energy.multiply(BigDecimal.valueOf(outputFactor(device.getDeviceType())))
                    .multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.9, 1.1)))
                    .setScale(2, RoundingMode.HALF_UP);
            String productType = (hourStart.getDayOfYear() + device.getId().intValue()) % 2 == 0 ? "A型壳体" : "B型支架";
            BigDecimal ambientTemp = BigDecimal.valueOf(
                    18 + 6 * Math.sin((hourStart.getMonthValue() - 1) / 12.0 * 2 * Math.PI)
                            + 3 * Math.sin(hour / 24.0 * 2 * Math.PI)
                            + device.getId() % 3)
                    .setScale(2, RoundingMode.HALF_UP);

            ProductionRecord record = new ProductionRecord();
            record.setEnterpriseId(device.getEnterpriseId());
            record.setWorkshopId(device.getWorkshopId());
            record.setLineId(device.getLineId());
            record.setDeviceId(device.getId());
            record.setRecordDate(hourStart.toLocalDate());
            record.setShift(shift);
            record.setStartTime(hourStart);
            record.setEndTime(hourStart.plusHours(1));
            record.setProductType(productType);
            record.setOutputQty(output);
            record.setOutputUnit("件");
            record.setRunMinutes(runMinutes);
            record.setLoadRate(loadRate);
            record.setAmbientTemp(ambientTemp);
            record.setStatus(status);
            record.setDataSource("SIMULATOR");

            ProductionRecord existing = productionMapper.selectOne(new LambdaQueryWrapper<ProductionRecord>()
                    .eq(ProductionRecord::getDeviceId, deviceId)
                    .eq(ProductionRecord::getStartTime, hourStart)
                    .eq(ProductionRecord::getShift, shift));
            if (existing == null) {
                productionMapper.insert(record);
            } else {
                existing.setOutputQty(output);
                existing.setLoadRate(loadRate);
                existing.setRunMinutes(runMinutes);
                existing.setAmbientTemp(ambientTemp);
                existing.setProductType(productType);
                existing.setStatus(status);
                productionMapper.updateById(existing);
            }
        }
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

    private double outputFactor(String deviceType) {
        if (deviceType == null) {
            return 2.0;
        }
        return switch (deviceType) {
            case "注塑机" -> 2.2;
            case "机械手" -> 5.0;
            case "涂覆机" -> 1.2;
            default -> 2.0;
        };
    }

    private BigDecimal lastReading(Long meterId, LocalDateTime before) {
        EnergyRawData last = rawMapper.selectOne(new LambdaQueryWrapper<EnergyRawData>()
                .eq(EnergyRawData::getMeterId, meterId)
                .lt(EnergyRawData::getCollectTime, before)
                .orderByDesc(EnergyRawData::getCollectTime)
                .last("LIMIT 1"));
        return last == null ? BigDecimal.ZERO : last.getRawValue();
    }

    private BigDecimal energyFor(EnergyDevice device, LocalDateTime time, int stepMinutes) {
        double rated = device != null && device.getRatedPower() != null
                ? device.getRatedPower().doubleValue() : 10.0;
        double baseKw = rated * 0.35;
        int hour = time.getHour();
        double shiftFactor = (hour >= 8 && hour < 12) || (hour >= 13 && hour < 17) ? 1.0
                : (hour >= 7 && hour < 22 ? 0.6 : 0.25);
        double weekFactor = time.getDayOfWeek() == DayOfWeek.SATURDAY ? 0.7
                : time.getDayOfWeek() == DayOfWeek.SUNDAY ? 0.4 : 1.0;
        int status = device == null || device.getStatus() == null ? 1 : device.getStatus();
        double statusFactor = switch (status) {
            case 2 -> 0.03;
            case 3 -> 0.10;
            case 0 -> 0.02;
            default -> 1.0;
        };
        double fluctuation = ThreadLocalRandom.current().nextDouble(0.85, 1.15);
        double kwh = baseKw * (stepMinutes / 60.0) * shiftFactor * weekFactor * statusFactor * fluctuation;
        return BigDecimal.valueOf(kwh).setScale(4, RoundingMode.HALF_UP);
    }
}
