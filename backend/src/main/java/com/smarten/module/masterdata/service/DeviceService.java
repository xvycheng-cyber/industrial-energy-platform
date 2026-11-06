package com.smarten.module.masterdata.service;

import com.smarten.common.api.PageResult;
import com.smarten.module.masterdata.dto.DeviceDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.vo.DeviceVO;

public interface DeviceService {

    PageResult<DeviceVO> page(MasterQueryDTO query);

    DeviceVO detail(Long id);

    Long create(DeviceDTO dto);

    void update(Long id, DeviceDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}
