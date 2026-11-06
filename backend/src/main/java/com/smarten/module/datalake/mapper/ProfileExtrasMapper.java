package com.smarten.module.datalake.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 画像所需的跨表只读查询（基线对比、异常分布、节能趋势）。
 *
 * <p>对应表在 P5/P6/P8 才会写入，当前返回空集合，前端展示「待接入」。</p>
 */
@Mapper
public interface ProfileExtrasMapper {

    @Select("""
            SELECT DATE_FORMAT(period_start, '%H:%i') AS label,
                   baseline_value AS baseline,
                   actual_value   AS actual,
                   deviation_rate AS deviation
            FROM energy_baseline_detail
            WHERE deleted = 0
              AND dim_type = #{dimType}
              AND dim_id = #{dimId}
              AND period_start >= #{start}
              AND period_start < #{end}
            ORDER BY period_start
            """)
    List<Map<String, Object>> baselinePoints(@Param("dimType") Integer dimType,
                                             @Param("dimId") Long dimId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Select("""
            SELECT anomaly_type AS name, COUNT(*) AS cnt
            FROM energy_anomaly
            WHERE deleted = 0
              AND enterprise_id = #{enterpriseId}
              AND detect_time >= #{start}
              AND detect_time < #{end}
            GROUP BY anomaly_type
            ORDER BY cnt DESC
            """)
    List<Map<String, Object>> anomalyDistribution(@Param("enterpriseId") Long enterpriseId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE_FORMAT(verify_start, '%Y-%m') AS month,
                   SUM(saving_value)  AS saving_energy,
                   SUM(saving_amount) AS saving_amount
            FROM energy_saving_result
            WHERE deleted = 0
              AND enterprise_id = #{enterpriseId}
              AND status = 1
              AND verify_start >= #{start}
            GROUP BY DATE_FORMAT(verify_start, '%Y-%m')
            ORDER BY month
            """)
    List<Map<String, Object>> savingTrend(@Param("enterpriseId") Long enterpriseId,
                                          @Param("start") LocalDateTime start);
}
