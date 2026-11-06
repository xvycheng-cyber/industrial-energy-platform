package com.smarten.module.datalake.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarten.module.datalake.entity.EnergyRawData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface EnergyRawDataMapper extends BaseMapper<EnergyRawData> {

    /** 按仪表统计应采点数（基于采集间隔，按天） */
    List<Map<String, Object>> countExpectedByMeter(@Param("enterpriseId") Long enterpriseId,
                                                   @Param("date") LocalDate date);

    /** 按仪表统计有效点数（quality <> 3 且已清洗） */
    List<Map<String, Object>> countValidByMeter(@Param("enterpriseId") Long enterpriseId,
                                                @Param("date") LocalDate date);
}
