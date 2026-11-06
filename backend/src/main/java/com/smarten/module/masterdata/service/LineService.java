package com.smarten.module.masterdata.service;

import com.smarten.common.api.PageResult;
import com.smarten.module.masterdata.dto.LineDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.vo.LineVO;

public interface LineService {

    PageResult<LineVO> page(MasterQueryDTO query);

    LineVO detail(Long id);

    Long create(LineDTO dto);

    void update(Long id, LineDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}
