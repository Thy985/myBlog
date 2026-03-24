package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.CommentLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT * FROM t_comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    CommentLike selectByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Delete("DELETE FROM t_comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 批量查询用户已点赞的评论ID（优化N+1查询）
     */
    @Select("<script>" +
            "SELECT comment_id FROM t_comment_like WHERE user_id = #{userId} AND comment_id IN " +
            "<foreach collection='commentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> selectLikedCommentIdsByUserId(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    /**
     * 批量查询用户点赞状态Map（优化N+1查询）
     */
    default Map<Long, Boolean> selectLikedCommentIds(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> likedIds = selectLikedCommentIdsByUserId(userId, commentIds);
        return likedIds.stream().collect(Collectors.toMap(id -> id, id -> true));
    }
}
