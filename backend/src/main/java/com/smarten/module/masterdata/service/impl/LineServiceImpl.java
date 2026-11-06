package com.smarten.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.masterdata.dto.LineDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.entity.EnergyWorkshop;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.masterdata.mapper.EnergyWorkshopMapper;
import com.smarten.module.masterdata.service.LineService;
import com.smarten.module.masterdata.vo.LineVO;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LineServiceImpl implements LineService {

    private final EnergyLineMapper lineMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final MasterNameResolver nameResolver;
    private final DataScopeHelper dataScope;

    @Override
    public PageResult<LineVO> page(MasterQueryDTO query) {
        LambdaQueryWrapper<EnergyLine> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(EnergyLine::getCode, keyword)
                    .or().like(EnergyLine::getName, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergyLine::getStatus, query.getStatus());
        }
        if (query.getWorkshopId() != null) {
            wrapper.eq(EnergyLine::getWorkshopId, query.getWorkshopId());
        }
        if (dataScope.isPlatformAdmin()) {
            if (query.getEnterpriseId() != null) {
                wrapper.eq(EnergyLine::getEnterpriseId, query.getEnterpriseId());
            }
        } else {
            wrapper.eq(EnergyLine::getEnterpriseId, dataScope.requireEnterpriseId());
        }
        wrapper.orderByDesc(EnergyLine::getId);

        Page<EnergyLine> page = lineMapper.selectPage(query.toPage(), wrapper);
        Map<Long, String> enterpriseNames = nameResolver.enterpriseNames(
                MasterNameResolver.ids(page.getRecords(), EnergyLine::getEnterpriseId));
        Map<Long, String> workshopNames = nameResolver.workshopNames(
                MasterNameResolver.ids(page.getRecords(), EnergyLine::getWorkshopId));

        List<LineVO> records = page.getRecords().stream()
                .map(item -> toVO(item, enterpriseNames, workshopNames))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public LineVO detail(Long id) {
        EnergyLine line = requireLine(id);
        dataScope.checkAccessible(line.getEnterpriseId());
        Map<Long, String> enterpriseNames = nameResolver.enterpriseNames(List.of(line.getEnterpriseId()));
        Map<Long, String> workshopNames = nameResolver.workshopNames(List.of(line.getWorkshopId()));
        return toVO(line, enterpriseNames, workshopNames);
    }

    @Override
    public Long create(LineDTO dto) {
        Long enterpriseId = dataScope.resolveEnterpriseId(dto.getEnterpriseId());
        EnergyWorkshop workshop = workshopMapper.selectById(dto.getWorkshopId());
        if (workshop == null) {
            throw new BusinessException(ResultCode.WORKSHOP_NOT_FOUND);
        }
        if (enterpriseId == null) {
            enterpriseId = workshop.getEnterpriseId();
        }
        if (!enterpriseId.equals(workshop.getEnterpriseId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "产线所属车间与所属企业不一致");
        }
        dataScope.checkAccessible(enterpriseId);

        Long exists = lineMapper.selectCount(new LambdaQueryWrapper<EnergyLine>()
                .eq(EnergyLine::getEnterpriseId, enterpriseId)
                .eq(EnergyLine::getCode, dto.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "产线编码已存在");
        }

        EnergyLine line = new EnergyLine();
        line.setEnterpriseId(enterpriseId);
        line.setWorkshopId(dto.getWorkshopId());
        line.setCode(dto.getCode());
        line.setName(dto.getName());
        line.setProductType(dto.getProductType());
        line.setDesignCapacity(dto.getDesignCapacity());
        line.setCapacityUnit(dto.getCapacityUnit());
        line.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        line.setRemark(dto.getRemark());
        lineMapper.insert(line);
        return line.getId();
    }

    @Override
    public void update(Long id, LineDTO dto) {
        EnergyLine line = requireLine(id);
        dataScope.checkAccessible(line.getEnterpriseId());

        EnergyWorkshop workshop = workshopMapper.selectById(dto.getWorkshopId());
        if (workshop == null) {
            throw new BusinessException(ResultCode.WORKSHOP_NOT_FOUND);
        }
        if (!line.getEnterpriseId().equals(workshop.getEnterpriseId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "产线所属车间与所属企业不一致");
        }
        Long exists = lineMapper.selectCount(new LambdaQueryWrapper<EnergyLine>()
                .eq(EnergyLine::getEnterpriseId, line.getEnterpriseId())
                .eq(EnergyLine::getCode, dto.getCode())
                .ne(EnergyLine::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "产线编码已存在");
        }
        line.setWorkshopId(dto.getWorkshopId());
        line.setCode(dto.getCode());
        line.setName(dto.getName());
        line.setProductType(dto.getProductType());
        line.setDesignCapacity(dto.getDesignCapacity());
        line.setCapacityUnit(dto.getCapacityUnit());
        line.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            line.setStatus(dto.getStatus());
        }
        lineMapper.updateById(line);
    }

    @Override
    public void delete(Long id) {
        EnergyLine line = requireLine(id);
        dataScope.checkAccessible(line.getEnterpriseId());
        Long devices = deviceMapper.selectCount(new LambdaQueryWrapper<EnergyDevice>()
                .eq(EnergyDevice::getLineId, id));
        if (devices != null && devices > 0) {
            throw new BusinessException(ResultCode.DATA_IN_USE, "产线下存在设备，无法删除");
        }
        lineMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        EnergyLine line = requireLine(id);
        dataScope.checkAccessible(line.getEnterpriseId());
        line.setStatus(status);
        lineMapper.updateById(line);
    }

    private EnergyLine requireLine(Long id) {
        EnergyLine line = lineMapper.selectById(id);
        if (line == null) {
            throw new BusinessException(ResultCode.LINE_NOT_FOUND);
        }
        return line;
    }

    private LineVO toVO(EnergyLine line, Map<Long, String> enterpriseNames, Map<Long, String> workshopNames) {
        LineVO vo = new LineVO();
        vo.setId(line.getId());
        vo.setEnterpriseId(line.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(line.getEnterpriseId()));
        vo.setWorkshopId(line.getWorkshopId());
        vo.setWorkshopName(workshopNames.get(line.getWorkshopId()));
        vo.setCode(line.getCode());
        vo.setName(line.getName());
        vo.setProductType(line.getProductType());
        vo.setDesignCapacity(line.getDesignCapacity());
        vo.setCapacityUnit(line.getCapacityUnit());
        vo.setStatus(line.getStatus());
        vo.setRemark(line.getRemark());
        vo.setCreateTime(line.getCreateTime());
        return vo;
    }
}
