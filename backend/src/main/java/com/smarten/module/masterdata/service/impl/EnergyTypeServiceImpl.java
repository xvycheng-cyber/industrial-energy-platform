package com.smarten.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.masterdata.dto.EnergyTypeDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.entity.EnergyType;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import com.smarten.module.masterdata.mapper.EnergyTypeMapper;
import com.smarten.module.masterdata.service.EnergyTypeService;
import com.smarten.module.masterdata.vo.EnergyTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnergyTypeServiceImpl implements EnergyTypeService {

    private final EnergyTypeMapper energyTypeMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyMeterMapper meterMapper;

    @Override
    public PageResult<EnergyTypeVO> page(MasterQueryDTO query) {
        LambdaQueryWrapper<EnergyType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(EnergyType::getCode, keyword).or().like(EnergyType::getName, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergyType::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(EnergyType::getId);

        Page<EnergyType> page = energyTypeMapper.selectPage(query.toPage(), wrapper);
        List<EnergyTypeVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public EnergyTypeVO detail(Long id) {
        return toVO(requireEnergyType(id));
    }

    @Override
    public Long create(EnergyTypeDTO dto) {
        Long exists = energyTypeMapper.selectCount(new LambdaQueryWrapper<EnergyType>()
                .eq(EnergyType::getCode, dto.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "能源类型编码已存在");
        }
        EnergyType type = new EnergyType();
        type.setCode(dto.getCode());
        type.setName(dto.getName());
        type.setUnit(dto.getUnit());
        type.setPrice(dto.getPrice() == null ? BigDecimal.ZERO : dto.getPrice());
        type.setCo2Factor(dto.getCo2Factor());
        type.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        energyTypeMapper.insert(type);
        return type.getId();
    }

    @Override
    public void update(Long id, EnergyTypeDTO dto) {
        EnergyType type = requireEnergyType(id);
        Long exists = energyTypeMapper.selectCount(new LambdaQueryWrapper<EnergyType>()
                .eq(EnergyType::getCode, dto.getCode())
                .ne(EnergyType::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "能源类型编码已存在");
        }
        type.setCode(dto.getCode());
        type.setName(dto.getName());
        type.setUnit(dto.getUnit());
        if (dto.getPrice() != null) {
            type.setPrice(dto.getPrice());
        }
        type.setCo2Factor(dto.getCo2Factor());
        if (dto.getStatus() != null) {
            type.setStatus(dto.getStatus());
        }
        energyTypeMapper.updateById(type);
    }

    @Override
    public void delete(Long id) {
        requireEnergyType(id);
        Long devices = deviceMapper.selectCount(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getEnergyTypeId, id));
        Long meters = meterMapper.selectCount(new LambdaQueryWrapper<EnergyMeter>()
                .eq(EnergyMeter::getEnergyTypeId, id));
        if ((devices != null && devices > 0) || (meters != null && meters > 0)) {
            throw new BusinessException(ResultCode.DATA_IN_USE, "能源类型已被设备或计量点引用，无法删除");
        }
        energyTypeMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        EnergyType type = requireEnergyType(id);
        type.setStatus(status);
        energyTypeMapper.updateById(type);
    }

    private EnergyType requireEnergyType(Long id) {
        EnergyType type = energyTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "能源类型不存在");
        }
        return type;
    }

    private EnergyTypeVO toVO(EnergyType type) {
        EnergyTypeVO vo = new EnergyTypeVO();
        vo.setId(type.getId());
        vo.setCode(type.getCode());
        vo.setName(type.getName());
        vo.setUnit(type.getUnit());
        vo.setPrice(type.getPrice());
        vo.setCo2Factor(type.getCo2Factor());
        vo.setStatus(type.getStatus());
        vo.setCreateTime(type.getCreateTime());
        return vo;
    }
}
