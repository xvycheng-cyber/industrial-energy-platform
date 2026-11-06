package com.smarten.module.masterdata.service;

import com.smarten.common.api.PageResult;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.dto.WorkshopDTO;
import com.smarten.module.masterdata.vo.WorkshopVO;

public interface WorkshopService {

    PageResult<WorkshopVO> page(MasterQueryDTO query);

    WorkshopVO detail(Long id);

    Long create(WorkshopDTO dto);

    void update(Long id, WorkshopDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}
