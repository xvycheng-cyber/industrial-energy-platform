package com.smarten.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.masterdata.dto.EnterpriseDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.entity.EnergyEnterprise;
import com.smarten.module.masterdata.entity.EnergyWorkshop;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyEnterpriseMapper;
import com.smarten.module.masterdata.mapper.EnergyWorkshopMapper;
import com.smarten.module.masterdata.service.EnterpriseService;
import com.smarten.module.masterdata.vo.EnterpriseVO;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EnterpriseServiceImpl implements EnterpriseService {

    private final EnergyEnterpriseMapper enterpriseMapper;
    private final EnergyWorkshopMapper workshopMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final DataScopeHelper dataScope;

    @Override
    public PageResult<EnterpriseVO> page(MasterQueryDTO query) {
        LambdaQueryWrapper<EnergyEnterprise> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(EnergyEnterprise::getCode, keyword)
                    .or().like(EnergyEnterprise::getName, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(EnergyEnterprise::getStatus, query.getStatus());
        }
        if (dataScope.isPlatformAdmin()) {
            if (query.getEnterpriseId() != null) {
                wrapper.eq(EnergyEnterprise::getId, query.getEnterpriseId());
            }
        } else {
            wrapper.eq(EnergyEnterprise::getId, dataScope.requireEnterpriseId());
        }
        wrapper.orderByDesc(EnergyEnterprise::getId);

        Page<EnergyEnterprise> page = enterpriseMapper.selectPage(query.toPage(), wrapper);
        List<EnterpriseVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public EnterpriseVO detail(Long id) {
        EnergyEnterprise enterprise = requireEnterprise(id);
        dataScope.checkAccessible(enterprise.getId());
        return toVO(enterprise);
    }

    @Override
    public Long create(EnterpriseDTO dto) {
        requirePlatformAdmin("只有平台管理员可以新增企业");
        Long exists = enterpriseMapper.selectCount(new LambdaQueryWrapper<EnergyEnterprise>()
                .eq(EnergyEnterprise::getCode, dto.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "企业编码已存在");
        }
        EnergyEnterprise enterprise = new EnergyEnterprise();
        enterprise.setCode(dto.getCode());
        enterprise.setName(dto.getName());
        enterprise.setIndustry(dto.getIndustry());
        enterprise.setRegion(dto.getRegion());
        enterprise.setAddress(dto.getAddress());
        enterprise.setContactName(dto.getContactName());
        enterprise.setContactPhone(dto.getContactPhone());
        enterprise.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        enterprise.setRemark(dto.getRemark());
        enterpriseMapper.insert(enterprise);
        return enterprise.getId();
    }

    @Override
    public void update(Long id, EnterpriseDTO dto) {
        EnergyEnterprise enterprise = requireEnterprise(id);
        dataScope.checkAccessible(id);

        Long exists = enterpriseMapper.selectCount(new LambdaQueryWrapper<EnergyEnterprise>()
                .eq(EnergyEnterprise::getCode, dto.getCode())
                .ne(EnergyEnterprise::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CODE_DUPLICATED, "企业编码已存在");
        }
        enterprise.setCode(dto.getCode());
        enterprise.setName(dto.getName());
        enterprise.setIndustry(dto.getIndustry());
        enterprise.setRegion(dto.getRegion());
        enterprise.setAddress(dto.getAddress());
        enterprise.setContactName(dto.getContactName());
        enterprise.setContactPhone(dto.getContactPhone());
        enterprise.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            enterprise.setStatus(dto.getStatus());
        }
        enterpriseMapper.updateById(enterprise);
    }

    @Override
    public void delete(Long id) {
        requirePlatformAdmin("只有平台管理员可以删除企业");
        requireEnterprise(id);
        Long workshops = workshopMapper.selectCount(new LambdaQueryWrapper<EnergyWorkshop>()
                .eq(EnergyWorkshop::getEnterpriseId, id));
        Long devices = deviceMapper.selectCount(new LambdaQueryWrapper<com.smarten.module.masterdata.entity.EnergyDevice>()
                .eq(com.smarten.module.masterdata.entity.EnergyDevice::getEnterpriseId, id));
        if ((workshops != null && workshops > 0) || (devices != null && devices > 0)) {
            throw new BusinessException(ResultCode.DATA_IN_USE, "企业下存在车间或设备，无法删除");
        }
        enterpriseMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        EnergyEnterprise enterprise = requireEnterprise(id);
        dataScope.checkAccessible(id);
        enterprise.setStatus(status);
        enterpriseMapper.updateById(enterprise);
    }

    private void requirePlatformAdmin(String message) {
        if (!dataScope.isPlatformAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, message);
        }
    }

    private EnergyEnterprise requireEnterprise(Long id) {
        EnergyEnterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
        }
        return enterprise;
    }

    private EnterpriseVO toVO(EnergyEnterprise enterprise) {
        EnterpriseVO vo = new EnterpriseVO();
        vo.setId(enterprise.getId());
        vo.setCode(enterprise.getCode());
        vo.setName(enterprise.getName());
        vo.setIndustry(enterprise.getIndustry());
        vo.setRegion(enterprise.getRegion());
        vo.setAddress(enterprise.getAddress());
        vo.setContactName(enterprise.getContactName());
        vo.setContactPhone(enterprise.getContactPhone());
        vo.setStatus(enterprise.getStatus());
        vo.setRemark(enterprise.getRemark());
        vo.setCreateTime(enterprise.getCreateTime());
        return vo;
    }
}
