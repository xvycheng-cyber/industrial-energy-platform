package com.smarten.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.dto.MeterDTO;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.entity.EnergyType;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import com.smarten.module.masterdata.mapper.EnergyTypeMapper;
import com.smarten.module.masterdata.service.MeterService;
import com.smarten.module.masterdata.vo.MeterVO;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeterServiceImpl implements MeterService {

    private final EnergyMeterMapper meterMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyTypeMapper energyTypeMapper;
    private final MasterNameResolver nameResolver;
    private final DataScopeHelper dataScope;

    @Override
    public PageResult<MeterVO> page(MasterQueryDTO query) {
        LambdaQueryWrapper<EnergyMeter> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(EnergyMeter::getMeterCode, keyword)
                    .or().like(EnergyMeter::getMeterName, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergyMeter::getStatus, query.getStatus());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(EnergyMeter::getDeviceId, query.getDeviceId());
        }
        if (query.getLineId() != null) {
            wrapper.eq(EnergyMeter::getLineId, query.getLineId());
        }
        if (dataScope.isPlatformAdmin()) {
            if (query.getEnterpriseId() != null) {
                wrapper.eq(EnergyMeter::getEnterpriseId, query.getEnterpriseId());
            }
        } else {
            wrapper.eq(EnergyMeter::getEnterpriseId, dataScope.requireEnterpriseId());
        }
        wrapper.orderByDesc(EnergyMeter::getId);

        Page<EnergyMeter> page = meterMapper.selectPage(query.toPage(), wrapper);
        List<MeterVO> records = toVOList(page.getRecords());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public MeterVO detail(Long id) {
        EnergyMeter meter = requireMeter(id);
        dataScope.checkAccessible(meter.getEnterpriseId());
        return toVOList(List.of(meter)).get(0);
    }

    @Override
    public Long create(MeterDTO dto) {
        Long enterpriseId = dataScope.resolveEnterpriseId(dto.getEnterpriseId());
        Long lineId = dto.getLineId();

        if (dto.getDeviceId() != null) {
            EnergyDevice device = deviceMapper.selectById(dto.getDeviceId());
            if (device == null) {
                throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
            }
            if (enterpriseId == null) {
                enterpriseId = device.getEnterpriseId();
            }
            if (!enterpriseId.equals(device.getEnterpriseId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "计量点绑定设备与企业不一致");
            }
            lineId = device.getLineId();
        } else if (lineId != null) {
            EnergyLine line = lineMapper.selectById(lineId);
            if (line == null) {
                throw new BusinessException(ResultCode.LINE_NOT_FOUND);
            }
            if (enterpriseId == null) {
                enterpriseId = line.getEnterpriseId();
            }
            if (!enterpriseId.equals(line.getEnterpriseId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "计量点绑定产线与企业不一致");
            }
        }
        if (enterpriseId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请指定所属企业或绑定设备");
        }
        dataScope.checkAccessible(enterpriseId);
        if (energyTypeMapper.selectById(dto.getEnergyTypeId()) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "能源类型不存在");
        }

        Long exists = meterMapper.selectCount(new LambdaQueryWrapper<EnergyMeter>()
                .eq(EnergyMeter::getMeterCode, dto.getMeterCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "仪表编号已存在");
        }

        EnergyMeter meter = new EnergyMeter();
        meter.setEnterpriseId(enterpriseId);
        meter.setDeviceId(dto.getDeviceId());
        meter.setLineId(lineId);
        meter.setMeterCode(dto.getMeterCode());
        meter.setMeterName(dto.getMeterName());
        meter.setEnergyTypeId(dto.getEnergyTypeId());
        meter.setRatio(dto.getRatio() == null ? BigDecimal.ONE : dto.getRatio());
        meter.setInstallTime(dto.getInstallTime());
        meter.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        meter.setRemark(dto.getRemark());
        meterMapper.insert(meter);
        return meter.getId();
    }

    @Override
    public void update(Long id, MeterDTO dto) {
        EnergyMeter meter = requireMeter(id);
        dataScope.checkAccessible(meter.getEnterpriseId());

        Long lineId = dto.getLineId();
        if (dto.getDeviceId() != null) {
            EnergyDevice device = deviceMapper.selectById(dto.getDeviceId());
            if (device == null) {
                throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
            }
            if (!meter.getEnterpriseId().equals(device.getEnterpriseId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "计量点绑定设备与企业不一致");
            }
            lineId = device.getLineId();
        } else if (lineId != null) {
            EnergyLine line = lineMapper.selectById(lineId);
            if (line == null) {
                throw new BusinessException(ResultCode.LINE_NOT_FOUND);
            }
            if (!meter.getEnterpriseId().equals(line.getEnterpriseId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "计量点绑定产线与企业不一致");
            }
        }
        if (energyTypeMapper.selectById(dto.getEnergyTypeId()) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "能源类型不存在");
        }
        Long exists = meterMapper.selectCount(new LambdaQueryWrapper<EnergyMeter>()
                .eq(EnergyMeter::getMeterCode, dto.getMeterCode())
                .ne(EnergyMeter::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "仪表编号已存在");
        }

        meter.setDeviceId(dto.getDeviceId());
        meter.setLineId(lineId);
        meter.setMeterCode(dto.getMeterCode());
        meter.setMeterName(dto.getMeterName());
        meter.setEnergyTypeId(dto.getEnergyTypeId());
        if (dto.getRatio() != null) {
            meter.setRatio(dto.getRatio());
        }
        meter.setInstallTime(dto.getInstallTime());
        meter.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            meter.setStatus(dto.getStatus());
        }
        meterMapper.updateById(meter);
    }

    @Override
    public void delete(Long id) {
        EnergyMeter meter = requireMeter(id);
        dataScope.checkAccessible(meter.getEnterpriseId());
        meterMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        EnergyMeter meter = requireMeter(id);
        dataScope.checkAccessible(meter.getEnterpriseId());
        meter.setStatus(status);
        meterMapper.updateById(meter);
    }

    private EnergyMeter requireMeter(Long id) {
        EnergyMeter meter = meterMapper.selectById(id);
        if (meter == null) {
            throw new BusinessException(ResultCode.METER_NOT_FOUND);
        }
        return meter;
    }

    private List<MeterVO> toVOList(List<EnergyMeter> meters) {
        if (meters.isEmpty()) {
            return List.of();
        }
        Map<Long, String> enterpriseNames = nameResolver.enterpriseNames(
                MasterNameResolver.ids(meters, EnergyMeter::getEnterpriseId));
        Map<Long, String> deviceNames = nameResolver.deviceNames(
                MasterNameResolver.ids(meters, EnergyMeter::getDeviceId));
        Map<Long, String> lineNames = nameResolver.lineNames(
                MasterNameResolver.ids(meters, EnergyMeter::getLineId));
        Map<Long, String> energyTypeNames = nameResolver.energyTypeNames(
                MasterNameResolver.ids(meters, EnergyMeter::getEnergyTypeId));

        return meters.stream().map(meter -> {
            MeterVO vo = new MeterVO();
            vo.setId(meter.getId());
            vo.setEnterpriseId(meter.getEnterpriseId());
            vo.setEnterpriseName(enterpriseNames.get(meter.getEnterpriseId()));
            vo.setDeviceId(meter.getDeviceId());
            vo.setDeviceName(meter.getDeviceId() == null ? null : deviceNames.get(meter.getDeviceId()));
            vo.setLineId(meter.getLineId());
            vo.setLineName(meter.getLineId() == null ? null : lineNames.get(meter.getLineId()));
            vo.setMeterCode(meter.getMeterCode());
            vo.setMeterName(meter.getMeterName());
            vo.setEnergyTypeId(meter.getEnergyTypeId());
            vo.setEnergyTypeName(energyTypeNames.get(meter.getEnergyTypeId()));
            vo.setRatio(meter.getRatio());
            vo.setInstallTime(meter.getInstallTime());
            vo.setStatus(meter.getStatus());
            vo.setRemark(meter.getRemark());
            vo.setCreateTime(meter.getCreateTime());
            return vo;
        }).toList();
    }
}
