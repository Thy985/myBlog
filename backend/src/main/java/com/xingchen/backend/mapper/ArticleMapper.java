package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Deprecated
    @Select("SELECT * FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 ORDER BY top_status DESC, publish_time DESC")
    List<Article> selectPublishedArticles();

    /**
     * 使用 QueryWrapper 分页查询已发布文章
     */
    default Page<Article> selectPublishedArticlesPage(int page, int size) {
        QueryWrapper wrapper = QueryWrapper.create()
            .eq("status", 1)
            .eq("view_status", 1)
            .eq("is_deleted", 0)
            .orderBy("top_status", false)
            .orderBy("publish_time", false);
        return paginate(page, size, wrapper);
    }

    // 按用户ID分页查询已发布文章
    @Select("SELECT * FROM t_article WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    // 按关键字分页查询已发布文章（标题+描述）
    @Select("SELECT * FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY top_status DESC, publish_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectByKeyword(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    // 搜索文章（标题+描述+内容）
    @Select("SELECT * FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY top_status DESC, publish_time DESC LIMIT #{offset}, #{size}")
    List<Article> searchByKeyword(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    //根据关键字查询文章数
    @Select("SELECT COUNT(*) FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))")
    long countByKeyword(@Param("keyword") String keyword);

    /**
     * 使用 JOIN 替代子查询查询分类文章（性能优化）
     */
    @Select("SELECT a.* FROM t_article a " +
            "INNER JOIN t_article_category ac ON a.id = ac.article_id " +
            "WHERE ac.category_id = #{categoryId} " +
            "AND a.status = 1 AND a.view_status = 1 AND a.is_deleted = 0 " +
            "ORDER BY a.top_status DESC, a.publish_time DESC " +
            "LIMIT #{offset}, #{size}")
    List<Article> selectByCategoryId(@Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM t_article a " +
            "INNER JOIN t_article_category ac ON a.id = ac.article_id " +
            "WHERE ac.category_id = #{categoryId} " +
            "AND a.status = 1 AND a.view_status = 1 AND a.is_deleted = 0")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 使用 JOIN 替代子查询查询标签文章（性能优化）
     */
    @Select("SELECT a.* FROM t_article a " +
            "INNER JOIN t_article_tag at ON a.id = at.article_id " +
            "WHERE at.tag_id = #{tagId} " +
            "AND a.status = 1 AND a.view_status = 1 AND a.is_deleted = 0 " +
            "ORDER BY a.top_status DESC, a.publish_time DESC " +
            "LIMIT #{offset}, #{size}")
    List<Article> selectByTagId(@Param("tagId") Long tagId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM t_article a " +
            "INNER JOIN t_article_tag at ON a.id = at.article_id " +
            "WHERE at.tag_id = #{tagId} " +
            "AND a.status = 1 AND a.view_status = 1 AND a.is_deleted = 0")
    long countByTagId(@Param("tagId") Long tagId);

    // 统计已发布文章数
    @Select("SELECT COUNT(*) FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0")
    long countPublicArticles();

    // 统计用户文章数
    @Select("SELECT COUNT(*) FROM t_article WHERE user_id = #{userId} AND is_deleted = 0")
    long countByUserId(@Param("userId") Long userId);

    // 统计用户已发布文章数
    @Select("SELECT COUNT(*) FROM t_article WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0")
    long countPublishedByUserId(@Param("userId") Long userId);

    // 按用户ID和关键字分页查询已发布文章（标题+描述）
    @Select("SELECT * FROM t_article WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    // 按用户ID和关键字查询文章数
    @Select("SELECT COUNT(*) FROM t_article WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))")
    long countByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    // 按用户ID和分类ID分页查询已发布文章（标题+描述）
    @Select("SELECT * FROM t_article WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND id IN (SELECT article_id FROM t_article_category WHERE category_id = #{categoryId}) " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectByUserIdAndCategoryId(@Param("userId") Long userId, @Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("size") int size);

    // 按用户ID和分类ID查询文章数
    @Select("SELECT COUNT(*) FROM t_article WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND id IN (SELECT article_id FROM t_article_category WHERE category_id = #{categoryId})")
    long countByUserIdAndCategoryId(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    // 按用户ID和标签ID分页查询已发布文章（标题+描述）
    @Select("SELECT * FROM t_article WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND id IN (SELECT article_id FROM t_article_tag WHERE tag_id = #{tagId}) " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectByUserIdAndTagId(@Param("userId") Long userId, @Param("tagId") Long tagId, @Param("offset") int offset, @Param("size") int size);

    // 按用户ID和标签ID查询文章数
    @Select("SELECT COUNT(*) FROM t_article WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND id IN (SELECT article_id FROM t_article_tag WHERE tag_id = #{tagId})")
    long countByUserIdAndTagId(@Param("userId") Long userId, @Param("tagId") Long tagId);

    // 更新文章状态
    @Update("UPDATE t_article SET read_num = read_num + 1 WHERE id = #{id}")
    int incrementReadNum(@Param("id") Long id);

    @Update("UPDATE t_article SET like_num = like_num + 1 WHERE id = #{id}")
    int incrementLikeNum(@Param("id") Long id);

    // 取消文章点赞
    @Update("UPDATE t_article SET like_num = like_num - 1 WHERE id = #{id} AND like_num > 0")
    int decrementLikeNum(@Param("id") Long id);

    // 添加文章评论数
    @Update("UPDATE t_article SET comment_num = comment_num + 1 WHERE id = #{id}")
    int incrementCommentNum(@Param("id") Long id);

    @Update("UPDATE t_article SET comment_num = comment_num - 1 WHERE id = #{id} AND comment_num > 0")
    int decrementCommentNum(@Param("id") Long id);

    @Update("UPDATE t_article SET collect_num = collect_num + 1 WHERE id = #{id}")
    int incrementCollectNum(@Param("id") Long id);

    @Update("UPDATE t_article SET collect_num = collect_num - 1 WHERE id = #{id} AND collect_num > 0")
    int decrementCollectNum(@Param("id") Long id);

    // 文章分享数
    @Update("UPDATE t_article SET share_num = share_num + 1 WHERE id = #{id}")
    int incrementShareNum(@Param("id") Long id);

    // 文章点赞数
    @Select("SELECT * FROM t_article WHERE status = 1 AND is_deleted = 0 ORDER BY read_num DESC LIMIT #{limit}")
    List<Article> selectHotArticles(@Param("limit") Integer limit);

    @Select("SELECT * FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 ORDER BY top_status DESC, publish_time DESC")
    List<Article> selectPublicArticles();

    // 分页查询已发布文章
    @Select("SELECT * FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 ORDER BY top_status DESC, publish_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectPublicArticlesPage(@Param("offset") int offset, @Param("size") int size);

    // 上一篇文章
    @Select("SELECT * FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 AND id < #{id} ORDER BY id DESC LIMIT 1")
    Article selectPreArticle(@Param("id") Long id);

    // 下一篇文章
    @Select("SELECT * FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 AND id > #{id} ORDER BY id ASC LIMIT 1")
    Article selectNextArticle(@Param("id") Long id);

    // 获取文章标题建议
    @Select("SELECT DISTINCT title FROM t_article WHERE status = 1 AND view_status = 1 AND is_deleted = 0 AND title LIKE CONCAT('%', #{keyword}, '%') LIMIT #{limit}")
    List<String> selectSuggestionTitles(@Param("keyword") String keyword, @Param("limit") Integer limit);

    /**
     * 按日期统计文章数
     */
    @Select("SELECT COUNT(*) FROM t_article WHERE create_time >= #{start} AND create_time < #{end} AND is_deleted = 0")
    int countByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按状态统计文章数
     */
    @Select("SELECT COUNT(*) FROM t_article WHERE status = #{status} AND is_deleted = 0")
    int countByStatus(@Param("status") Integer status);

    /**
     * 查询热门文章（带作者信息）
     */
    @Select("SELECT a.id, a.title, a.read_num as viewCount, u.username as author " +
            "FROM t_article a " +
            "LEFT JOIN t_user u ON a.user_id = u.id " +
            "WHERE a.status = 1 AND a.is_deleted = 0 " +
            "ORDER BY a.read_num DESC LIMIT #{limit}")
    List<Map<String, Object>> selectHotArticlesWithAuthor(@Param("limit") Integer limit);

    /**
     * 统计文章总数
     */
    @Select("SELECT COUNT(*) FROM t_article WHERE is_deleted = 0")
    long countAll();

    /**
     * 查询最近发布的N篇文章（用于管理后台仪表盘）
     */
    @Select("SELECT * FROM t_article WHERE is_deleted = 0 ORDER BY publish_time DESC LIMIT #{limit}")
    List<Article> selectRecentArticles(@Param("limit") Integer limit);

    /**
     * 查询所有已发布文章（用于AI知识库索引）
     */
    @Select("SELECT * FROM t_article WHERE status = 1 AND is_deleted = 0")
    List<Article> selectAllPublished();
}
