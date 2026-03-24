package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.LoginHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LoginHistoryMapper extends BaseMapper<LoginHistory> {
    
    @org.apache.ibatis.annotations.Select("SELECT * FROM t_login_history WHERE user_id = #{userId} ORDER BY login_time DESC LIMIT #{limit}")
    List<LoginHistory> selectByUserId(Long userId, Integer limit);
}