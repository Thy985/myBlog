package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.ArticleTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {
    // 查询指定文章的所有标签
    @Select("SELECT * FROM t_article_tag WHERE article_id = #{articleId}")
    List<ArticleTag> selectByArticleId(@Param("articleId") Long articleId);

    // 查询指定文章的所有标签ID
    @Select("SELECT tag_id FROM t_article_tag WHERE article_id = #{articleId}")
    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);

    // 批量查询指定文章的所有标签
    @Select("<script>" +
            "SELECT * FROM t_article_tag WHERE article_id IN " +
            "<foreach item='id' collection='articleIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<ArticleTag> selectByArticleIds(@Param("articleIds") List<Long> articleIds);

    // 批量查询指定文章的所有标签ID
    @Delete("DELETE FROM t_article_tag WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
