package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("SELECT * FROM t_category WHERE is_deleted = 0 AND status = 1 ORDER BY sort_order ASC, create_time DESC")
    List<Category> selectAllActive();

    // 根据父级分类ID查询子级分类
    @Select("SELECT * FROM t_category WHERE parent_id = #{parentId} AND is_deleted = 0 AND status = 1 ORDER BY sort_order ASC, create_time DESC")
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    // 更新分类文章数量
    @Update("UPDATE t_category SET article_count = article_count + 1 WHERE id = #{id}")
    int incrementArticleCount(@Param("id") Long id);

    @Update("UPDATE t_category SET article_count = article_count - 1 WHERE id = #{id} AND article_count > 0")
    int decrementArticleCount(@Param("id") Long id);

    // 更新分类状态
    @Update("UPDATE t_category SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    // 查询分类下的文章和数量
    @Select("SELECT c.id, c.category_name as name, COUNT(ac.article_id) as count " +
            "FROM t_category c " +
            "LEFT JOIN t_article_category ac ON c.id = ac.category_id " +
            "LEFT JOIN t_article a ON ac.article_id = a.id AND a.is_deleted = 0 " +
            "WHERE c.is_deleted = 0 " +
            "GROUP BY c.id, c.category_name " +
            "ORDER BY count DESC")
    List<Map<String, Object>> selectArticleCountGroupByCategory();

    @Select("SELECT COUNT(*) FROM t_category WHERE is_deleted = 0")
    long countAll();
}
