package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.ArticleContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleContentMapper extends BaseMapper<ArticleContent> {

    @Select("SELECT * FROM t_article_content WHERE article_id = #{articleId}")
    ArticleContent selectByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT content FROM t_article_content WHERE article_id = #{articleId}")
    String selectContentByArticleId(@Param("articleId") Long articleId);
}
