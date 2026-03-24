package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.PageView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface PageViewMapper extends BaseMapper<PageView> {

    /**
     * 统计所有页面浏览记录总数
     * @return 页面浏览总数
     */
    @Select("SELECT COUNT(*) FROM t_page_view")
    Long countAll();

    /**
     * 根据时间范围统计页面浏览量(PV)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指定时间范围内的PV数量
     */
    @Select("SELECT COUNT(*) FROM t_page_view WHERE create_time >= #{startTime} AND create_time < #{endTime}")
    Long selectPvCount(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据时间范围统计独立访客数(UV)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指定时间范围内的UV数量
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM t_page_view WHERE create_time >= #{startTime} AND create_time < #{endTime} AND user_id IS NOT NULL")
    Long selectUvCount(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据时间范围统计独立IP数
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指定时间范围内的独立IP数量
     */
    @Select("SELECT COUNT(DISTINCT ip) FROM t_page_view WHERE create_time >= #{startTime} AND create_time < #{endTime} AND ip IS NOT NULL")
    Long selectIpCount(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计所有独立用户数
     * @return 所有独立用户总数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM t_page_view WHERE user_id IS NOT NULL")
    Long countDistinctUsers();

    /**
     * 统计所有独立IP数
     * @return 所有独立IP总数
     */
    @Select("SELECT COUNT(DISTINCT ip) FROM t_page_view WHERE ip IS NOT NULL")
    Long countDistinctIps();

    /**
     * 根据日期统计页面浏览量
     * @param date 指定日期
     * @return 该日期的页面浏览量
     */
    @Select("SELECT COUNT(*) FROM t_page_view WHERE DATE(create_time) = #{date}")
    Long countByDate(@Param("date") LocalDate date);

    /**
     * 根据日期统计独立用户数
     * @param date 指定日期
     * @return 该日期的独立用户数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM t_page_view WHERE DATE(create_time) = #{date} AND user_id IS NOT NULL")
    Long countDistinctUsersByDate(@Param("date") LocalDate date);

    /**
     * 统计指定时间之后的页面浏览量
     * @param since 起始时间
     * @return 从指定时间开始的页面浏览总量
     */
    @Select("SELECT COUNT(*) FROM t_page_view WHERE create_time >= #{since}")
    Long countSince(@Param("since") LocalDateTime since);

    /**
     * 统计指定时间之后的活跃用户数
     * @param since 起始时间
     * @return 从指定时间开始的活跃用户总数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM t_page_view WHERE create_time >= #{since} AND user_id IS NOT NULL")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * 按设备类型统计访问量
     * @return 设备类型及其对应的访问次数映射
     */
    @Select("SELECT " +
            "CASE " +
            "WHEN user_agent LIKE '%Mobile%' OR user_agent LIKE '%Android%' OR user_agent LIKE '%iPhone%' THEN 'Mobile' " +
            "WHEN user_agent LIKE '%Tablet%' OR user_agent LIKE '%iPad%' THEN 'Tablet' " +
            "ELSE 'Desktop' " +
            "END as device_type, COUNT(*) as count " +
            "FROM t_page_view " +
            "GROUP BY device_type")
    Map<String, Long> countByDeviceType();

    /**
     * 按来源网站统计访问量
     * @return 来源网站及其对应的访问次数映射
     */
    @Select("SELECT referer, COUNT(*) as count FROM t_page_view GROUP BY referer")
    Map<String, Long> countByReferer();

    /**
     * 获取指定日期范围内的趋势数据(每日PV和UV)
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含日期、PV、UV的数据列表
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as pv, COUNT(DISTINCT user_id) as uv " +
            "FROM t_page_view WHERE create_time >= #{startDate} AND create_time < #{endDate} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> selectTrendData(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取浏览器使用统计(前10名)
     * @param startTime 统计起始时间
     * @return 浏览器名称及使用次数的列表
     */
    @Select("SELECT browser, COUNT(*) as count FROM t_page_view " +
            "WHERE create_time >= #{startTime} GROUP BY browser ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> selectBrowserStats(@Param("startTime") LocalDateTime startTime);

    /**
     * 获取操作系统使用统计(前10名)
     * @param startTime 统计起始时间
     * @return 操作系统名称及使用次数的列表
     */
    @Select("SELECT os, COUNT(*) as count FROM t_page_view " +
            "WHERE create_time >= #{startTime} GROUP BY os ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> selectOsStats(@Param("startTime") LocalDateTime startTime);

    /**
     * 获取访问来源统计
     * @param startTime 统计起始时间
     * @return 访问来源类型及访问次数的列表
     */
    @Select("SELECT CASE WHEN referer IS NULL OR referer = '' THEN '直接访问' ELSE '外部链接' END as source, COUNT(*) as count " +
            "FROM t_page_view WHERE create_time >= #{startTime} GROUP BY source ORDER BY count DESC")
    List<Map<String, Object>> selectSourceStats(@Param("startTime") LocalDateTime startTime);
}
