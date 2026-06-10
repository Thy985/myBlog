package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.ArticleContent;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ArticleContentMapper extends BaseMapper<ArticleContent> {

    @Select("SELECT * FROM t_article_content WHERE article_id = #{articleId}")
    ArticleContent selectByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT content FROM t_article_content WHERE article_id = #{articleId}")
    String selectContentByArticleId(@Param("articleId") Long articleId);

    @Insert("INSERT INTO t_article_content (article_id, content, word_count) " +
            "VALUES (#{articleId}, #{content}, #{wordCount}) " +
            "ON DUPLICATE KEY UPDATE content = #{content}, word_count = #{wordCount}")
    int upsert(@Param("articleId") Long articleId,
               @Param("content") String content,
               @Param("wordCount") Integer wordCount);
}
