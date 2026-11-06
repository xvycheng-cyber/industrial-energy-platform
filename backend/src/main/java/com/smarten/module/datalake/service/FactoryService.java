package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.datalake.entity.EnergyAggregate;
import com.smarten.module.datalake.entity.ProductionRecord;
import com.smarten.module.datalake.mapper.EnergyAggregateMapper;
import com.smarten.module.datalake.mapper.ProductionRecordMapper;
import com.smarten.module.datalake.vo.FactoryDeviceDetailVO;
import com.smarten.module.datalake.vo.FactoryDeviceNodeVO;
import com.smarten.module.datalake.vo.FactoryLineDetailVO;
import com.smarten.module.datalake.vo.FactoryLineNodeVO;
import com.smarten.module.datalake.vo.FactorySceneVO;
import com.smarten.module.datalake.vo.FactoryWorkshopNodeVO;
import com.smarten.module.datalake.vo.Vec3VO;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 三维工厂：场景布局、设备/产线详情。
 *
 * <p>坐标由后端统一计算，前端只负责渲染，保证两端布局一致。</p>
 */
@Service
@RequiredArgsConstructor
public class FactoryService {

    public static final String RISK_NORMAL = "NORMAL";
    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";
    public static final String RISK_OFFLINE = "OFFLINE";

    private static final double WORKSHOP_WIDTH = 18;
    private static final double WORKSHOP_DEPTH = 12;
    private static final double WORKSHOP_HEIGHT = 6;
    private static final double WORKSHOP_GAP = 10;
    private static final double LINE_SPACING = 4.5;
    private static final double DEVICE_SPACING = 3.2;

    private final EnergyEnterpriseMapper enterpriseMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyAggregateMapper aggregateMapper;
    private final ProductionRecordMapper productionMapper;
    private final DataScopeHelper dataScope;

    public FactorySceneVO scene(Long enterpriseId) {
        Long scope = dataScope.resolveEnterpriseId(enterpriseId);
        if (scope == null) {
            List<EnergyEnterprise> first = enterpriseMapper.selectList(new LambdaQueryWrapper<EnergyEnterprise>()
                    .eq(EnergyEnterprise::getStatus, 1).orderByAsc(EnergyEnterprise::getId).last("LIMIT 1"));
            if (first.isEmpty()) {
                throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND, "暂无企业数据");
            }
            scope = first.get(0).getId();
        }
        dataScope.checkAccessible(scope);
        EnergyEnterprise enterprise = enterpriseMapper.selectById(scope);
        if (enterprise == null) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
        }
        LocalDate today = LocalDate.now();
        Map<Long, BigDecimal> deviceEnergy = energyByDim(scope, 4, today);
        Map<Long, BigDecimal> lineEnergy = energyByDim(scope, 3, today);
        Map<Long, BigDecimal> workshopEnergy = energyByDim(scope, 2, today);

        List<EnergyWorkshop> workshops = workshopMapper.selectList(new LambdaQueryWrapper<EnergyWorkshop>()
                .eq(EnergyWorkshop::getEnterpriseId, scope)
                .orderByAsc(EnergyWorkshop::getId));

        FactorySceneVO scene = new FactorySceneVO();
        scene.setEnterpriseId(scope);
        scene.setEnterpriseName(enterprise.getName());
        scene.setDataTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        int index = 0;
        int lineCount = 0;
        int deviceCount = 0;
        BigDecimal totalEnergy = BigDecimal.ZERO;
        for (EnergyWorkshop workshop : workshops) {
            FactoryWorkshopNodeVO node = new FactoryWorkshopNodeVO();
            node.setId(workshop.getId());
            node.setCode(workshop.getCode());
            node.setName(workshop.getName());
            node.setWidth(WORKSHOP_WIDTH);
            node.setDepth(WORKSHOP_DEPTH);
            node.setHeight(WORKSHOP_HEIGHT);
            double workshopX = (index - (workshops.size() - 1) / 2.0) * (WORKSHOP_WIDTH + WORKSHOP_GAP);
            node.setPosition(new Vec3VO(workshopX, 0, 0));
            node.setEnergyToday(scale(workshopEnergy.get(workshop.getId())));

            List<EnergyLine> lines = lineMapper.selectList(new LambdaQueryWrapper<EnergyLine>()
                    .eq(EnergyLine::getWorkshopId, workshop.getId())
                    .orderByAsc(EnergyLine::getId));
            int lineIndex = 0;
            for (EnergyLine line : lines) {
                FactoryLineNodeVO lineNode = new FactoryLineNodeVO();
                lineNode.setId(line.getId());
                lineNode.setCode(line.getCode());
                lineNode.setName(line.getName());
                lineNode.setEnergyToday(scale(lineEnergy.get(line.getId())));
                double lineZ = lineIndex * LINE_SPACING - ((lines.size() - 1) * LINE_SPACING) / 2.0;
                lineNode.setPosition(new Vec3VO(0, 0.08, lineZ));

                List<EnergyDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<EnergyDevice>()
                        .eq(EnergyDevice::getLineId, line.getId())
                        .orderByAsc(EnergyDevice::getId));
                int deviceIndex = 0;
                for (EnergyDevice device : devices) {
                    FactoryDeviceNodeVO deviceNode = new FactoryDeviceNodeVO();
                    deviceNode.setId(device.getId());
                    deviceNode.setCode(device.getCode());
                    deviceNode.setName(device.getName());
                    deviceNode.setDeviceType(device.getDeviceType());
                    deviceNode.setStatus(device.getStatus());
                    BigDecimal energy = scale(deviceEnergy.get(device.getId()));
                    deviceNode.setEnergyToday(energy);
                    applyRisk(deviceNode, device.getStatus(), energy);
                    double x = deviceIndex * DEVICE_SPACING - ((devices.size() - 1) * DEVICE_SPACING) / 2.0;
                    deviceNode.setPosition(new Vec3VO(x, 0, 0));
                    lineNode.getDevices().add(deviceNode);
                    deviceIndex++;
                    deviceCount++;
                }
                node.getLines().add(lineNode);
                lineIndex++;
                lineCount++;
            }
            scene.getWorkshops().add(node);
            totalEnergy = totalEnergy.add(node.getEnergyToday());
            index++;
        }

        scene.setWorkshopCount(workshops.size());
        scene.setLineCount(lineCount);
        scene.setDeviceCount(deviceCount);
        scene.setEnergyToday(scale(totalEnergy));
        scene.setWidth(Math.max(24, workshops.size() * (WORKSHOP_WIDTH + WORKSHOP_GAP) - WORKSHOP_GAP + 6));
        scene.setDepth(WORKSHOP_DEPTH + 16);
        return scene;
    }

    public FactoryDeviceDetailVO deviceDetail(Long id) {
        EnergyDevice device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        dataScope.checkAccessible(device.getEnterpriseId());
        LocalDate today = LocalDate.now();
        Map<Long, BigDecimal> energy = energyByDim(device.getEnterpriseId(), 4, today);
        BigDecimal todayEnergy = scale(energy.get(id));

        FactoryDeviceDetailVO vo = new FactoryDeviceDetailVO();
        vo.setId(device.getId());
        vo.setCode(device.getCode());
        vo.setName(device.getName());
        vo.setDeviceType(device.getDeviceType());
        vo.setModel(device.getModel());
        vo.setRatedPower(device.getRatedPower());
        vo.setStatus(device.getStatus());
        vo.setStatusLabel(statusLabel(device.getStatus()));
        vo.setEnergyToday(todayEnergy);
        vo.setUpdatedAt(LocalDateTime.now());

        FactoryDeviceNodeVO node = new FactoryDeviceNodeVO();
        applyRisk(node, device.getStatus(), todayEnergy);
        vo.setRisk(node.getRisk());
        vo.setRiskLabel(node.getRiskLabel());
        vo.setRiskReason(node.getRiskReason());

        EnergyLine line = device.getLineId() == null ? null : lineMapper.selectById(device.getLineId());
        vo.setLineName(line == null ? null : line.getName());
        EnergyWorkshop workshop = device.getWorkshopId() == null ? null : workshopMapper.selectById(device.getWorkshopId());
        vo.setWorkshopName(workshop == null ? null : workshop.getName());

        // 动态基线与异常诊断在 P5 / P6 接入，此处明确标记不可用，不用估算值冒充
        vo.setBaselineAvailable(false);
        vo.setAnomalyAvailable(false);
        return vo;
    }

    public FactoryLineDetailVO lineDetail(Long id) {
        EnergyLine line = lineMapper.selectById(id);
        if (line == null) {
            throw new BusinessException(ResultCode.LINE_NOT_FOUND);
        }
        dataScope.checkAccessible(line.getEnterpriseId());
        LocalDate today = LocalDate.now();

        FactoryLineDetailVO vo = new FactoryLineDetailVO();
        vo.setId(line.getId());
        vo.setCode(line.getCode());
        vo.setName(line.getName());
        EnergyWorkshop workshop = line.getWorkshopId() == null ? null : workshopMapper.selectById(line.getWorkshopId());
        vo.setWorkshopName(workshop == null ? null : workshop.getName());
        vo.setEnergyToday(scale(energyByDim(line.getEnterpriseId(), 3, today).get(id)));

        Map<String, Integer> riskCounts = new LinkedHashMap<>();
        riskCounts.put(RISK_NORMAL, 0);
        riskCounts.put(RISK_LOW, 0);
        riskCounts.put(RISK_MEDIUM, 0);
        riskCounts.put(RISK_HIGH, 0);
        riskCounts.put(RISK_OFFLINE, 0);

        Map<Long, BigDecimal> deviceEnergy = energyByDim(line.getEnterpriseId(), 4, today);
        List<EnergyDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getLineId, id));
        int running = 0;
        for (EnergyDevice device : devices) {
            FactoryDeviceNodeVO node = new FactoryDeviceNodeVO();
            applyRisk(node, device.getStatus(), scale(deviceEnergy.get(device.getId())));
            riskCounts.merge(node.getRisk(), 1, Integer::sum);
            if (device.getStatus() != null && device.getStatus() == 1) {
                running++;
            }
        }
        vo.setDeviceCount(devices.size());
        vo.setRunningCount(running);
        vo.setRiskCounts(riskCounts);

        Map<String, Object> production = productionSum("line_id", id, today);
        vo.setOutputQty(toDecimal(production.get("output_qty")));
        vo.setOutputUnit("件");

        vo.setBaselineAvailable(false);
        return vo;
    }

    private void applyRisk(FactoryDeviceNodeVO node, Integer status, BigDecimal energyToday) {
        if (status == null || status == 0 || status == 2) {
            node.setRisk(RISK_OFFLINE);
            node.setRiskLabel("离线");
            node.setRiskReason("设备已停机或停用");
            return;
        }
        if (status == 3) {
            node.setRisk(RISK_MEDIUM);
            node.setRiskLabel("中风险");
            node.setRiskReason("设备检修中");
            return;
        }
        if (energyToday == null || energyToday.signum() <= 0) {
            node.setRisk(RISK_LOW);
            node.setRiskLabel("低风险");
            node.setRiskReason("今日暂无能耗数据");
            return;
        }
        node.setRisk(RISK_NORMAL);
        node.setRiskLabel("正常");
        node.setRiskReason("运行正常，今日有能耗数据");
    }

    private Map<Long, BigDecimal> energyByDim(Long enterpriseId, int dimType, LocalDate day) {
        QueryWrapper<EnergyAggregate> wrapper = new QueryWrapper<>();
        wrapper.select("dim_id", "SUM(energy_value) AS energy_value")
                .eq("deleted", 0)
                .eq("enterprise_id", enterpriseId)
                .eq("dim_type", dimType)
                .eq("period_type", 2)
                .eq("period_start", day.atStartOfDay())
                .groupBy("dim_id");
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : aggregateMapper.selectMaps(wrapper)) {
            Object dimId = row.get("dim_id");
            if (dimId != null) {
                result.put(((Number) dimId).longValue(), toDecimal(row.get("energy_value")));
            }
        }
        return result;
    }

    private Map<String, Object> productionSum(String column, Long id, LocalDate day) {
        QueryWrapper<ProductionRecord> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(SUM(output_qty),0) AS output_qty")
                .eq("deleted", 0)
                .eq(column, id)
                .eq("record_date", day);
        List<Map<String, Object>> rows = productionMapper.selectMaps(wrapper);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "运行";
            case 2 -> "停机";
            case 3 -> "检修";
            case 0 -> "停用";
            default -> "未知";
        };
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }
}
