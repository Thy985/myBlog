package com.xingchen.backend.service.impl;

import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.entity.Notification;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.mapper.NotificationMapper;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.NotificationService;
import com.xingchen.backend.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<NotificationVO> getNotificationList(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Notification> notifications = notificationMapper.selectByUserId(userId, offset, size);
        List<NotificationVO> voList = notifications.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        Long total = notificationMapper.countByUserId(userId);
        int pages = (int) Math.ceil((double) total / size);
        return PageResult.of(voList, total, pages, page, size);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationMapper.selectOneById(id);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "无权限操作该通知");
        }
        int updated = notificationMapper.markAsRead(id, userId);
        if (updated == 0) {
            log.warn("标记已读无效，通知ID: {}, 用户ID: {}", id, userId);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllUnreadAsRead(userId);
    }

    @Override
    public void deleteNotification(Long id, Long userId) {
        Notification notification = notificationMapper.selectOneById(id);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "无权限删除该通知");
        }
        notificationMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String type, String title, String content, 
                                   Long relatedId, String relatedType, Long senderId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setSenderId(senderId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        
        notificationMapper.insert(notification);
        log.info("创建通知: 用户ID={}, 类型={}, 标题={}", userId, type, title);
    }

    @Override
    public void sendCommentNotification(Long articleAuthorId, Long commentId, Long commenterId, 
                                        String articleTitle, String commentContent) {
        User commenter = userMapper.selectOneById(commenterId);
        String commenterName = commenter != null ? commenter.getNickname() : "匿名用户";
        
        createNotification(
            articleAuthorId,
            "COMMENT",
            "新评论通知",
            String.format("%s 在文章《%s》中发表了评论：%s", 
                commenterName, 
                truncate(articleTitle, 20), 
                truncate(commentContent, 50)),
            commentId,
            "comment",
            commenterId
        );
    }

    @Override
    public void sendReplyNotification(Long userId, Long commentId, Long replierId, 
                                      String replyContent) {
        User replier = userMapper.selectOneById(replierId);
        String replierName = replier != null ? replier.getNickname() : "匿名用户";
        
        createNotification(
            userId,
            "REPLY",
            "回复通知",
            String.format("%s 回复了您的评论：%s", replierName, truncate(replyContent, 50)),
            commentId,
            "comment",
            replierId
        );
    }

    @Override
    public void sendLikeNotification(Long userId, Long articleId, Long likerId, String articleTitle) {
        User liker = userMapper.selectOneById(likerId);
        String likerName = liker != null ? liker.getNickname() : "匿名用户";
        
        createNotification(
            userId,
            "LIKE",
            "点赞通知",
            String.format("%s 赞了您的文章《%s》", likerName, truncate(articleTitle, 20)),
            articleId,
            "article",
            likerId
        );
    }

    @Override
    public void sendSystemNotification(Long userId, String title, String content) {
        createNotification(
            userId,
            "SYSTEM",
            title,
            content,
            null,
            null,
            null
        );
    }

    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);
        vo.setIsRead(notification.getIsRead() == 1);
        
        if (notification.getSenderId() != null) {
            User sender = userMapper.selectOneById(notification.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
        }
        
        return vo;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
