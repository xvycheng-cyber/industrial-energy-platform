package com.smarten.module.datalake.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarten.module.datalake.entity.EnergyAggregate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface EnergyAggregateMapper extends BaseMapper<EnergyAggregate> {

    int aggregateDevice(@Param("periodType") Integer periodType,
                        @Param("bucket") String bucket,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("version") String version);

    int aggregateLine(@Param("periodType") Integer periodType,
                      @Param("bucket") String bucket,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end,
                      @Param("version") String version);

    int aggregateWorkshop(@Param("periodType") Integer periodType,
                          @Param("bucket") String bucket,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("version") String version);

    int aggregateEnterprise(@Param("periodType") Integer periodType,
                            @Param("bucket") String bucket,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end,
                            @Param("version") String version);
}
