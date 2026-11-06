package com.smarten.module.masterdata.service;

import com.smarten.common.api.PageResult;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.dto.MeterDTO;
import com.smarten.module.masterdata.vo.MeterVO;

public interface MeterService {

    PageResult<MeterVO> page(MasterQueryDTO query);

    MeterVO detail(Long id);

    Long create(MeterDTO dto);

    void update(Long id, MeterDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}
