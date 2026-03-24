package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.FileCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FileCategoryMapper extends BaseMapper<FileCategory> {

    @Select("SELECT * FROM t_file_category WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<FileCategory> selectAllActive();

    @Update("UPDATE t_file_category SET file_count = file_count + 1 WHERE id = #{id}")
    int incrementFileCount(@Param("id") Long id);

    @Update("UPDATE t_file_category SET file_count = file_count - 1 WHERE id = #{id} AND file_count > 0")
    int decrementFileCount(@Param("id") Long id);
}
