package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.ArticleCategory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleCategoryMapper extends BaseMapper<ArticleCategory> {

    @Select("SELECT * FROM t_article_category WHERE article_id = #{articleId}")
    ArticleCategory selectByArticleId(@Param("articleId") Long articleId);

    // 批量查询文章分类
    @Select("<script>" +
            "SELECT * FROM t_article_category WHERE article_id IN " +
            "<foreach item='id' collection='articleIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<ArticleCategory> selectByArticleIds(@Param("articleIds") List<Long> articleIds);

    @Delete("DELETE FROM t_article_category WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
