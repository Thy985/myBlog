package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.DailyStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {

    @Select("SELECT * FROM t_daily_stats WHERE stats_date = #{date}")
    DailyStats selectByDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM t_daily_stats WHERE stats_date >= #{startDate} AND stats_date <= #{endDate} ORDER BY stats_date")
    List<DailyStats> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT SUM(pv) FROM t_daily_stats")
    Long selectTotalPv();

    @Select("SELECT SUM(uv) FROM t_daily_stats")
    Long selectTotalUv();
}
