package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    @Select("SELECT * FROM t_notification WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Notification> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);
    
    @Select("SELECT COUNT(*) FROM t_notification WHERE user_id = #{userId} AND is_read = 0")
    Long countUnread(@Param("userId") Long userId);
    
    @Select("SELECT COUNT(*) FROM t_notification WHERE user_id = #{userId}")
    Long countByUserId(@Param("userId") Long userId);
    
    @Update("UPDATE t_notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);
    
    @Update("UPDATE t_notification SET is_read = 1 WHERE user_id = #{userId}")
    int markAllAsRead(@Param("userId") Long userId);
    
    @Update("UPDATE t_notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllUnreadAsRead(@Param("userId") Long userId);
}
