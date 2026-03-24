package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT * FROM t_tag WHERE is_deleted = 0 AND status = 1 ORDER BY article_count DESC LIMIT #{limit}")
    List<Tag> selectHotTags(@Param("limit") Integer limit);

    @Select("SELECT t.* FROM t_tag t INNER JOIN t_article_tag at ON t.id = at.tag_id WHERE at.article_id = #{articleId} AND t.is_deleted = 0 AND t.status = 1")
    List<Tag> selectByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT * FROM t_tag WHERE is_deleted = 0 AND status = 1 ORDER BY create_time DESC")
    List<Tag> selectAllActive();

    @Update("UPDATE t_tag SET article_count = article_count + 1 WHERE id = #{id}")
    int incrementArticleCount(@Param("id") Long id);

    @Update("UPDATE t_tag SET article_count = article_count - 1 WHERE id = #{id} AND article_count > 0")
    int decrementArticleCount(@Param("id") Long id);

    @Update("UPDATE t_tag SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Select("SELECT t.id, t.tag_name as name, COUNT(at.article_id) as count FROM t_tag t " +
            "LEFT JOIN t_article_tag at ON t.id = at.tag_id " +
            "WHERE t.is_deleted = 0 GROUP BY t.id, t.tag_name ORDER BY count DESC")
    List<Map<String, Object>> selectArticleCountGroupByTag();

    @Select("SELECT COUNT(*) FROM t_tag WHERE is_deleted = 0")
    long countAll();
}
