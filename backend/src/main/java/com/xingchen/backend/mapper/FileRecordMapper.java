package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {

    @Select("SELECT * FROM t_file WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<FileRecord> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_file WHERE category_id = #{categoryId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<FileRecord> selectByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM t_file WHERE file_type = #{fileType} AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<FileRecord> selectByFileType(@Param("fileType") String fileType, @Param("limit") Integer limit);

    @Update("UPDATE t_file SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(@Param("id") Long id);

    @Select("SELECT SUM(file_size) FROM t_file WHERE user_id = #{userId} AND is_deleted = 0")
    Long selectTotalFileSizeByUserId(@Param("userId") Long userId);
}
