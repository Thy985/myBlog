package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.BlogSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BlogSettingMapper extends BaseMapper<BlogSetting> {
    // 根据key查询
    @Select("SELECT * FROM t_blog_setting WHERE setting_key = #{key}")
    BlogSetting selectByKey(@Param("key") String key);
}
