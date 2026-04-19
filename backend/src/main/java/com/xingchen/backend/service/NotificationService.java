package com.xingchen.backend.service;

import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.vo.NotificationVO;

import java.util.List;

public interface NotificationService {
    PageResult<NotificationVO> getNotificationList(Long userId, Integer page, Integer size);

    Long getUnreadCount(Long userId);

    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long id, Long userId);
    
    void createNotification(Long userId, String type, String title, String content, 
                           Long relatedId, String relatedType, Long senderId);
    
    void sendCommentNotification(Long articleAuthorId, Long commentId, Long commenterId, 
                                String articleTitle, String commentContent);
    
    void sendReplyNotification(Long userId, Long commentId, Long replierId, String replyContent);
    
    void sendLikeNotification(Long userId, Long articleId, Long likerId, String articleTitle);
    
    void sendSystemNotification(Long userId, String title, String content);

    /**
     * 批量删除通知
     * @param ids 通知ID列表
     * @param userId 用户ID
     */
    void deleteNotifications(List<Long> ids, Long userId);

    /**
     * 清空所有通知
     * @param userId 用户ID
     */
    void clearAllNotifications(Long userId);
}
