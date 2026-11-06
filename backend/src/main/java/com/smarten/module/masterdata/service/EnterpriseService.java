package com.smarten.module.masterdata.service;

import com.smarten.common.api.PageResult;
import com.smarten.module.masterdata.dto.EnterpriseDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.vo.EnterpriseVO;

public interface EnterpriseService {

    PageResult<EnterpriseVO> page(MasterQueryDTO query);

    EnterpriseVO detail(Long id);

    Long create(EnterpriseDTO dto);

    void update(Long id, EnterpriseDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}
