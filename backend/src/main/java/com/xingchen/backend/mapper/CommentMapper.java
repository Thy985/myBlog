package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT * FROM t_comment WHERE article_id = #{articleId} AND parent_id = 0 AND status = 1 AND is_deleted = 0 ORDER BY create_time DESC")
    List<Comment> selectRootCommentsByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT * FROM t_comment WHERE article_id = #{articleId} AND user_id = #{userId} AND status = 1 AND is_deleted = 0 ORDER BY id DESC LIMIT 1")
    Comment selectLastByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("SELECT LAST_INSERT_ID()")
    Long selectLastInsertId();

    @Select("SELECT * FROM t_comment WHERE root_id = #{rootId} AND parent_id != 0 AND status = 1 AND is_deleted = 0 ORDER BY create_time ASC")
    List<Comment> selectRepliesByRootId(@Param("rootId") Long rootId);

    /**
     * 批量查询多个根评论的所有回复（优化N+1查询）
     */
    @Select("<script>" +
            "SELECT * FROM t_comment WHERE root_id IN " +
            "<foreach collection='rootIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND parent_id != 0 AND status = 1 AND is_deleted = 0 ORDER BY create_time ASC" +
            "</script>")
    List<Comment> selectRepliesByRootIds(@Param("rootIds") List<Long> rootIds);

    @Select("SELECT * FROM t_comment WHERE article_id = #{articleId} AND status = 1 AND is_deleted = 0 ORDER BY create_time DESC")
    List<Comment> selectByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT * FROM t_comment WHERE status = 0 AND is_deleted = 0 ORDER BY create_time DESC")
    List<Comment> selectPendingComments();

    @Select("SELECT COUNT(*) FROM t_comment WHERE article_id = #{articleId} AND status = 1 AND is_deleted = 0")
    int countByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT COUNT(*) FROM t_comment WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0")
    int countByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_comment WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Comment> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Update("UPDATE t_comment SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeNum(@Param("id") Long id);

    @Update("UPDATE t_comment SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrementLikeNum(@Param("id") Long id);

    @Update("UPDATE t_comment SET reply_count = reply_count + 1 WHERE id = #{id}")
    int incrementReplyNum(@Param("id") Long id);

    @Update("UPDATE t_comment SET reply_count = reply_count - 1 WHERE id = #{id} AND reply_count > 0")
    int decrementReplyNum(@Param("id") Long id);

    /**
     * 按日期统计评论数
     */
    @Select("SELECT COUNT(*) FROM t_comment WHERE create_time >= #{start} AND create_time < #{end} AND is_deleted = 0")
    int countByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按状态统计评论数
     */
    @Select("SELECT COUNT(*) FROM t_comment WHERE status = #{status} AND is_deleted = 0")
    int countByStatus(@Param("status") Integer status);

    /**
     * 统计评论总数
     */
    @Select("SELECT COUNT(*) FROM t_comment WHERE is_deleted = 0")
    long countAll();
}
