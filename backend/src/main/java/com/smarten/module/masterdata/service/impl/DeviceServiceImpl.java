package com.smarten.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.masterdata.dto.DeviceDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import com.smarten.module.masterdata.service.DeviceService;
import com.smarten.module.masterdata.vo.DeviceVO;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final EnergyDeviceMapper deviceMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyMeterMapper meterMapper;
    private final MasterNameResolver nameResolver;
    private final DataScopeHelper dataScope;

    @Override
    public PageResult<DeviceVO> page(MasterQueryDTO query) {
        LambdaQueryWrapper<EnergyDevice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(EnergyDevice::getCode, keyword)
                    .or().like(EnergyDevice::getName, keyword)
                    .or().like(EnergyDevice::getModel, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergyDevice::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getDeviceType())) {
            wrapper.eq(EnergyDevice::getDeviceType, query.getDeviceType().trim());
        }
        if (query.getWorkshopId() != null) {
            wrapper.eq(EnergyDevice::getWorkshopId, query.getWorkshopId());
        }
        if (query.getLineId() != null) {
            wrapper.eq(EnergyDevice::getLineId, query.getLineId());
        }
        if (dataScope.isPlatformAdmin()) {
            if (query.getEnterpriseId() != null) {
                wrapper.eq(EnergyDevice::getEnterpriseId, query.getEnterpriseId());
            }
        } else {
            wrapper.eq(EnergyDevice::getEnterpriseId, dataScope.requireEnterpriseId());
        }
        wrapper.orderByDesc(EnergyDevice::getId);

        Page<EnergyDevice> page = deviceMapper.selectPage(query.toPage(), wrapper);
        List<DeviceVO> records = toVOList(page.getRecords());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public DeviceVO detail(Long id) {
        EnergyDevice device = requireDevice(id);
        dataScope.checkAccessible(device.getEnterpriseId());
        return toVOList(List.of(device)).get(0);
    }

    @Override
    public Long create(DeviceDTO dto) {
        Long enterpriseId = dataScope.resolveEnterpriseId(dto.getEnterpriseId());
        EnergyLine line = lineMapper.selectById(dto.getLineId());
        if (line == null) {
            throw new BusinessException(ResultCode.LINE_NOT_FOUND);
        }
        if (enterpriseId == null) {
            enterpriseId = line.getEnterpriseId();
        }
        if (!enterpriseId.equals(line.getEnterpriseId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "设备所属产线与企业不一致");
        }
        dataScope.checkAccessible(enterpriseId);

        Long exists = deviceMapper.selectCount(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getEnterpriseId, enterpriseId)
                .eq(EnergyDevice::getCode, dto.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "设备编码已存在");
        }

        EnergyDevice device = new EnergyDevice();
        device.setEnterpriseId(enterpriseId);
        device.setWorkshopId(line.getWorkshopId());
        device.setLineId(line.getId());
        device.setCode(dto.getCode());
        device.setName(dto.getName());
        device.setDeviceType(dto.getDeviceType());
        device.setModel(dto.getModel());
        device.setRatedPower(dto.getRatedPower());
        device.setEnergyTypeId(dto.getEnergyTypeId());
        device.setCommissionDate(dto.getCommissionDate());
        device.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        device.setRemark(dto.getRemark());
        deviceMapper.insert(device);
        return device.getId();
    }

    @Override
    public void update(Long id, DeviceDTO dto) {
        EnergyDevice device = requireDevice(id);
        dataScope.checkAccessible(device.getEnterpriseId());

        EnergyLine line = lineMapper.selectById(dto.getLineId());
        if (line == null) {
            throw new BusinessException(ResultCode.LINE_NOT_FOUND);
        }
        if (!device.getEnterpriseId().equals(line.getEnterpriseId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "设备所属产线与企业不一致");
        }
        Long exists = deviceMapper.selectCount(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getEnterpriseId, device.getEnterpriseId())
                .eq(EnergyDevice::getCode, dto.getCode())
                .ne(EnergyDevice::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "设备编码已存在");
        }

        device.setWorkshopId(line.getWorkshopId());
        device.setLineId(line.getId());
        device.setCode(dto.getCode());
        device.setName(dto.getName());
        device.setDeviceType(dto.getDeviceType());
        device.setModel(dto.getModel());
        device.setRatedPower(dto.getRatedPower());
        device.setEnergyTypeId(dto.getEnergyTypeId());
        device.setCommissionDate(dto.getCommissionDate());
        device.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            device.setStatus(dto.getStatus());
        }
        deviceMapper.updateById(device);
    }

    @Override
    public void delete(Long id) {
        EnergyDevice device = requireDevice(id);
        dataScope.checkAccessible(device.getEnterpriseId());
        Long meters = meterMapper.selectCount(new LambdaQueryWrapper<EnergyMeter>()
                .eq(EnergyMeter::getDeviceId, id));
        if (meters != null && meters > 0) {
            throw new BusinessException(ResultCode.DATA_IN_USE, "设备下存在计量点，无法删除");
        }
        deviceMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        EnergyDevice device = requireDevice(id);
        dataScope.checkAccessible(device.getEnterpriseId());
        device.setStatus(status);
        deviceMapper.updateById(device);
    }

    private EnergyDevice requireDevice(Long id) {
        EnergyDevice device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        return device;
    }

    private List<DeviceVO> toVOList(List<EnergyDevice> devices) {
        if (devices.isEmpty()) {
            return List.of();
        }
        Map<Long, String> enterpriseNames = nameResolver.enterpriseNames(
                MasterNameResolver.ids(devices, EnergyDevice::getEnterpriseId));
        Map<Long, String> workshopNames = nameResolver.workshopNames(
                MasterNameResolver.ids(devices, EnergyDevice::getWorkshopId));
        Map<Long, String> lineNames = nameResolver.lineNames(
                MasterNameResolver.ids(devices, EnergyDevice::getLineId));
        Map<Long, String> energyTypeNames = nameResolver.energyTypeNames(
                MasterNameResolver.ids(devices, EnergyDevice::getEnergyTypeId));

        return devices.stream().map(device -> {
            DeviceVO vo = new DeviceVO();
            vo.setId(device.getId());
            vo.setEnterpriseId(device.getEnterpriseId());
            vo.setEnterpriseName(enterpriseNames.get(device.getEnterpriseId()));
            vo.setWorkshopId(device.getWorkshopId());
            vo.setWorkshopName(workshopNames.get(device.getWorkshopId()));
            vo.setLineId(device.getLineId());
            vo.setLineName(lineNames.get(device.getLineId()));
            vo.setCode(device.getCode());
            vo.setName(device.getName());
            vo.setDeviceType(device.getDeviceType());
            vo.setModel(device.getModel());
            vo.setRatedPower(device.getRatedPower());
            vo.setEnergyTypeId(device.getEnergyTypeId());
            vo.setEnergyTypeName(device.getEnergyTypeId() == null ? null : energyTypeNames.get(device.getEnergyTypeId()));
            vo.setCommissionDate(device.getCommissionDate());
            vo.setStatus(device.getStatus());
            vo.setRemark(device.getRemark());
            vo.setCreateTime(device.getCreateTime());
            return vo;
        }).toList();
    }
}
