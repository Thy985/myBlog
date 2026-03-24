package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.ArticleReadStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleReadStatsMapper extends BaseMapper<ArticleReadStats> {
    // 查询指定文章指定日期的阅读统计数据
    @Select("SELECT * FROM t_article_read_stats WHERE article_id = #{articleId} AND stats_date = #{date}")
    ArticleReadStats selectByArticleAndDate(@Param("articleId") Long articleId, @Param("date") LocalDate date);

    // 查询指定文章指定日期范围内的阅读统计数据
    @Select("SELECT SUM(read_count) FROM t_article_read_stats WHERE article_id = #{articleId} AND stats_date >= #{startDate}")
    Integer sumReadCountSince(@Param("articleId") Long articleId, @Param("startDate") LocalDate startDate);

    // 查询指定文章指定日期范围内的阅读趋势数据
    @Select("SELECT stats_date as date, read_count as count FROM t_article_read_stats " +
            "WHERE article_id = #{articleId} AND stats_date >= #{startDate} AND stats_date <= #{endDate} " +
            "ORDER BY stats_date ASC")
    List<Map<String, Object>> selectTrendData(@Param("articleId") Long articleId, 
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    // 更新指定文章指定日期的阅读统计数据
    @Update("UPDATE t_article_read_stats SET read_count = read_count + 1, update_time = NOW() " +
            "WHERE article_id = #{articleId} AND stats_date = #{date}")
    int incrementReadCount(@Param("articleId") Long articleId, @Param("date") LocalDate date);
}
