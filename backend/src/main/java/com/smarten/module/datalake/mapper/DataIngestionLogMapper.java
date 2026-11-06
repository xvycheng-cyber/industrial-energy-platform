package com.smarten.module.datalake.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarten.module.datalake.entity.DataIngestionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataIngestionLogMapper extends BaseMapper<DataIngestionLog> {
}
