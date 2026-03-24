package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {

    @Select("SELECT keyword, COUNT(*) as count FROM t_search_history " +
            "WHERE create_time >= #{startTime} GROUP BY keyword ORDER BY count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectHotKeywords(@Param("startTime") LocalDateTime startTime, @Param("limit") Integer limit);

    @Select("SELECT * FROM t_search_history WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<SearchHistory> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM t_search_history WHERE keyword = #{keyword}")
    Long countByKeyword(@Param("keyword") String keyword);
}
