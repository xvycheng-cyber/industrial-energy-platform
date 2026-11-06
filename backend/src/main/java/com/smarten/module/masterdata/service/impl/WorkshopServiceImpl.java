package com.smarten.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.dto.WorkshopDTO;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.entity.EnergyWorkshop;
import com.smarten.module.masterdata.mapper.EnergyEnterpriseMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.masterdata.mapper.EnergyWorkshopMapper;
import com.smarten.module.masterdata.service.WorkshopService;
import com.smarten.module.masterdata.vo.WorkshopVO;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkshopServiceImpl implements WorkshopService {

    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyLineMapper lineMapper;
    private final EnergyEnterpriseMapper enterpriseMapper;
    private final MasterNameResolver nameResolver;
    private final DataScopeHelper dataScope;

    @Override
    public PageResult<WorkshopVO> page(MasterQueryDTO query) {
        LambdaQueryWrapper<EnergyWorkshop> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(EnergyWorkshop::getCode, keyword)
                    .or().like(EnergyWorkshop::getName, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergyWorkshop::getStatus, query.getStatus());
        }
        if (dataScope.isPlatformAdmin()) {
            if (query.getEnterpriseId() != null) {
                wrapper.eq(EnergyWorkshop::getEnterpriseId, query.getEnterpriseId());
            }
        } else {
            wrapper.eq(EnergyWorkshop::getEnterpriseId, dataScope.requireEnterpriseId());
        }
        wrapper.orderByDesc(EnergyWorkshop::getId);

        Page<EnergyWorkshop> page = workshopMapper.selectPage(query.toPage(), wrapper);
        List<Long> enterpriseIds = MasterNameResolver.ids(page.getRecords(), EnergyWorkshop::getEnterpriseId);
        Map<Long, String> enterpriseNames = nameResolver.enterpriseNames(enterpriseIds);
        List<WorkshopVO> records = page.getRecords().stream()
                .map(item -> toVO(item, enterpriseNames))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public WorkshopVO detail(Long id) {
        EnergyWorkshop workshop = requireWorkshop(id);
        dataScope.checkAccessible(workshop.getEnterpriseId());
        Map<Long, String> names = nameResolver.enterpriseNames(List.of(workshop.getEnterpriseId()));
        return toVO(workshop, names);
    }

    @Override
    public Long create(WorkshopDTO dto) {
        Long enterpriseId = dataScope.resolveEnterpriseId(dto.getEnterpriseId());
        if (enterpriseId == null) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND, "请选择所属企业");
        }
        if (enterpriseMapper.selectById(enterpriseId) == null) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
        }
        Long exists = workshopMapper.selectCount(new LambdaQueryWrapper<EnergyWorkshop>()
                .eq(EnergyWorkshop::getEnterpriseId, enterpriseId)
                .eq(EnergyWorkshop::getCode, dto.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "车间编码已存在");
        }
        EnergyWorkshop workshop = new EnergyWorkshop();
        workshop.setEnterpriseId(enterpriseId);
        workshop.setCode(dto.getCode());
        workshop.setName(dto.getName());
        workshop.setManager(dto.getManager());
        workshop.setArea(dto.getArea());
        workshop.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        workshop.setRemark(dto.getRemark());
        workshopMapper.insert(workshop);
        return workshop.getId();
    }

    @Override
    public void update(Long id, WorkshopDTO dto) {
        EnergyWorkshop workshop = requireWorkshop(id);
        dataScope.checkAccessible(workshop.getEnterpriseId());
        Long exists = workshopMapper.selectCount(new LambdaQueryWrapper<EnergyWorkshop>()
                .eq(EnergyWorkshop::getEnterpriseId, workshop.getEnterpriseId())
                .eq(EnergyWorkshop::getCode, dto.getCode())
                .ne(EnergyWorkshop::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "车间编码已存在");
        }
        workshop.setCode(dto.getCode());
        workshop.setName(dto.getName());
        workshop.setManager(dto.getManager());
        workshop.setArea(dto.getArea());
        workshop.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            workshop.setStatus(dto.getStatus());
        }
        workshopMapper.updateById(workshop);
    }

    @Override
    public void delete(Long id) {
        EnergyWorkshop workshop = requireWorkshop(id);
        dataScope.checkAccessible(workshop.getEnterpriseId());
        Long lines = lineMapper.selectCount(new LambdaQueryWrapper<EnergyLine>()
                .eq(EnergyLine::getWorkshopId, id));
        if (lines != null && lines > 0) {
            throw new BusinessException(ResultCode.DATA_IN_USE, "车间下存在产线，无法删除");
        }
        workshopMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        EnergyWorkshop workshop = requireWorkshop(id);
        dataScope.checkAccessible(workshop.getEnterpriseId());
        workshop.setStatus(status);
        workshopMapper.updateById(workshop);
    }

    private EnergyWorkshop requireWorkshop(Long id) {
        EnergyWorkshop workshop = workshopMapper.selectById(id);
        if (workshop == null) {
            throw new BusinessException(ResultCode.WORKSHOP_NOT_FOUND);
        }
        return workshop;
    }

    private WorkshopVO toVO(EnergyWorkshop workshop, Map<Long, String> enterpriseNames) {
        WorkshopVO vo = new WorkshopVO();
        vo.setId(workshop.getId());
        vo.setEnterpriseId(workshop.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(workshop.getEnterpriseId()));
        vo.setCode(workshop.getCode());
        vo.setName(workshop.getName());
        vo.setManager(workshop.getManager());
        vo.setArea(workshop.getArea());
        vo.setStatus(workshop.getStatus());
        vo.setRemark(workshop.getRemark());
        vo.setCreateTime(workshop.getCreateTime());
        return vo;
    }
}
