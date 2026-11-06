package com.smarten.module.masterdata.service;

import com.smarten.common.api.PageResult;
import com.smarten.module.masterdata.dto.EnergyTypeDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.vo.EnergyTypeVO;

public interface EnergyTypeService {

    PageResult<EnergyTypeVO> page(MasterQueryDTO query);

    EnergyTypeVO detail(Long id);

    Long create(EnergyTypeDTO dto);

    void update(Long id, EnergyTypeDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}
