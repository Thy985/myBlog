package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.ArticleLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    @Select("SELECT * FROM t_article_like WHERE article_id = #{articleId} AND user_id = #{userId}")
    ArticleLike selectByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Delete("DELETE FROM t_article_like WHERE article_id = #{articleId} AND user_id = #{userId}")
    int deleteByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_article_like WHERE article_id = #{articleId}")
    Long selectCountByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT COUNT(*) FROM t_article_like WHERE user_id = #{userId}")
    Long selectCountByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM t_article_like WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Select("<script>SELECT article_id FROM t_article_like WHERE user_id = #{userId} AND article_id IN <foreach collection='articleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Long> selectArticleIdsByUserAndArticleIds(@Param("userId") Long userId, @Param("articleIds") List<Long> articleIds);
}
