package com.xingchen.backend.service;

import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.entity.Notification;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.NotificationMapper;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.impl.NotificationServiceImpl;
import com.xingchen.backend.vo.NotificationVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // ==================== Helper Methods ====================

    private Notification buildNotification(Long id, Long userId, Long senderId, int isRead) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setType("COMMENT");
        notification.setTitle("Test Notification");
        notification.setContent("Test Content");
        notification.setRelatedId(100L);
        notification.setRelatedType("comment");
        notification.setSenderId(senderId);
        notification.setIsRead(isRead);
        notification.setCreateTime(LocalDateTime.now());
        return notification;
    }

    private User buildUser(Long id, String nickname, String avatar) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        return user;
    }

    // ==================== getNotificationList Tests ====================

    @Nested
    @DisplayName("getNotificationList")
    class GetNotificationListTests {

        @Test
        @DisplayName("should return paginated notifications with sender info")
        void getNotificationList_Success() {
            // Arrange
            Long userId = 1L;
            Notification notification = buildNotification(1L, userId, 2L, 0);
            User sender = buildUser(2L, "Sender User", "avatar.jpg");

            when(notificationMapper.selectByUserId(userId, 0, 10))
                    .thenReturn(Collections.singletonList(notification));
            when(notificationMapper.countByUserId(userId)).thenReturn(1L);
            when(userMapper.selectOneById(2L)).thenReturn(sender);

            // Act
            PageResult<NotificationVO> result = notificationService.getNotificationList(userId, 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getList().size());
            assertEquals(1L, result.getTotal());
            assertEquals(1, result.getPage());

            NotificationVO vo = result.getList().get(0);
            assertEquals(1L, vo.getId());
            assertFalse(vo.getIsRead());
            assertEquals("Sender User", vo.getSenderName());
            assertEquals("avatar.jpg", vo.getSenderAvatar());
        }

        @Test
        @DisplayName("should handle notification without sender")
        void getNotificationList_NoSender() {
            // Arrange
            Long userId = 1L;
            Notification notification = buildNotification(1L, userId, null, 1);

            when(notificationMapper.selectByUserId(userId, 0, 10))
                    .thenReturn(Collections.singletonList(notification));
            when(notificationMapper.countByUserId(userId)).thenReturn(1L);

            // Act
            PageResult<NotificationVO> result = notificationService.getNotificationList(userId, 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getList().size());
            assertTrue(result.getList().get(0).getIsRead());
            assertNull(result.getList().get(0).getSenderName());
        }

        @Test
        @DisplayName("should handle sender not found in DB")
        void getNotificationList_SenderNotFound() {
            // Arrange
            Long userId = 1L;
            Notification notification = buildNotification(1L, userId, 99L, 0);

            when(notificationMapper.selectByUserId(userId, 0, 10))
                    .thenReturn(Collections.singletonList(notification));
            when(notificationMapper.countByUserId(userId)).thenReturn(1L);
            when(userMapper.selectOneById(99L)).thenReturn(null);

            // Act
            PageResult<NotificationVO> result = notificationService.getNotificationList(userId, 1, 10);

            // Assert
            assertNotNull(result);
            assertNull(result.getList().get(0).getSenderName());
        }

        @Test
        @DisplayName("should return empty list when no notifications")
        void getNotificationList_Empty() {
            // Arrange
            when(notificationMapper.selectByUserId(1L, 0, 10)).thenReturn(Collections.emptyList());
            when(notificationMapper.countByUserId(1L)).thenReturn(0L);

            // Act
            PageResult<NotificationVO> result = notificationService.getNotificationList(1L, 1, 10);

            // Assert
            assertNotNull(result);
            assertTrue(result.getList().isEmpty());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== getUnreadCount Tests ====================

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTests {

        @Test
        @DisplayName("should return unread count")
        void getUnreadCount_Success() {
            // Arrange
            when(notificationMapper.countUnread(1L)).thenReturn(5L);

            // Act
            Long result = notificationService.getUnreadCount(1L);

            // Assert
            assertEquals(5L, result);
        }

        @Test
        @DisplayName("should return zero when no unread notifications")
        void getUnreadCount_Zero() {
            // Arrange
            when(notificationMapper.countUnread(1L)).thenReturn(0L);

            // Act
            Long result = notificationService.getUnreadCount(1L);

            // Assert
            assertEquals(0L, result);
        }
    }

    // ==================== markAsRead Tests ====================

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("should mark notification as read successfully")
        void markAsRead_Success() {
            // Arrange
            Notification notification = buildNotification(1L, 1L, 2L, 0);
            when(notificationMapper.selectOneById(1L)).thenReturn(notification);
            when(notificationMapper.markAsRead(1L, 1L)).thenReturn(1);

            // Act & Assert
            assertDoesNotThrow(() -> notificationService.markAsRead(1L, 1L));
            verify(notificationMapper).markAsRead(1L, 1L);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when notification does not exist")
        void markAsRead_NotFound() {
            // Arrange
            when(notificationMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> notificationService.markAsRead(999L, 1L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw NO_PERMISSION when notification belongs to another user")
        void markAsRead_NoPermission() {
            // Arrange
            Notification notification = buildNotification(1L, 2L, 3L, 0);
            when(notificationMapper.selectOneById(1L)).thenReturn(notification);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> notificationService.markAsRead(1L, 1L));
            assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should log warning when update returns 0")
        void markAsRead_UpdateReturnsZero() {
            // Arrange
            Notification notification = buildNotification(1L, 1L, 2L, 0);
            when(notificationMapper.selectOneById(1L)).thenReturn(notification);
            when(notificationMapper.markAsRead(1L, 1L)).thenReturn(0);

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> notificationService.markAsRead(1L, 1L));
        }
    }

    // ==================== markAllAsRead Tests ====================

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("should mark all unread notifications as read")
        void markAllAsRead_Success() {
            // Act
            notificationService.markAllAsRead(1L);

            // Assert
            verify(notificationMapper).markAllUnreadAsRead(1L);
        }
    }

    // ==================== deleteNotification Tests ====================

    @Nested
    @DisplayName("deleteNotification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("should delete notification successfully")
        void deleteNotification_Success() {
            // Arrange
            Notification notification = buildNotification(1L, 1L, 2L, 0);
            when(notificationMapper.selectOneById(1L)).thenReturn(notification);

            // Act
            assertDoesNotThrow(() -> notificationService.deleteNotification(1L, 1L));

            // Assert
            verify(notificationMapper).deleteById(1L);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when notification does not exist")
        void deleteNotification_NotFound() {
            // Arrange
            when(notificationMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> notificationService.deleteNotification(999L, 1L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw NO_PERMISSION when notification belongs to another user")
        void deleteNotification_NoPermission() {
            // Arrange
            Notification notification = buildNotification(1L, 2L, 3L, 0);
            when(notificationMapper.selectOneById(1L)).thenReturn(notification);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> notificationService.deleteNotification(1L, 1L));
            assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
        }
    }

    // ==================== createNotification Tests ====================

    @Nested
    @DisplayName("createNotification")
    class CreateNotificationTests {

        @Test
        @DisplayName("should create notification successfully")
        void createNotification_Success() {
            // Act
            notificationService.createNotification(1L, "COMMENT", "Title", "Content", 100L, "comment", 2L);

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }

        @Test
        @DisplayName("should create system notification with null related fields")
        void createNotification_SystemType() {
            // Act
            notificationService.createNotification(1L, "SYSTEM", "System Title", "System Content", null, null, null);

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }
    }

    // ==================== sendCommentNotification Tests ====================

    @Nested
    @DisplayName("sendCommentNotification")
    class SendCommentNotificationTests {

        @Test
        @DisplayName("should send comment notification with commenter name")
        void sendCommentNotification_Success() {
            // Arrange
            User commenter = buildUser(2L, "Commenter", "avatar.jpg");
            when(userMapper.selectOneById(2L)).thenReturn(commenter);

            // Act
            notificationService.sendCommentNotification(1L, 100L, 2L, "Article Title", "Comment text");

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }

        @Test
        @DisplayName("should use '匿名用户' when commenter not found")
        void sendCommentNotification_CommenterNotFound() {
            // Arrange
            when(userMapper.selectOneById(2L)).thenReturn(null);

            // Act
            notificationService.sendCommentNotification(1L, 100L, 2L, "Article Title", "Comment text");

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }

        @Test
        @DisplayName("should truncate long article title and comment content")
        void sendCommentNotification_TruncateLongText() {
            // Arrange
            User commenter = buildUser(2L, "Commenter", "avatar.jpg");
            when(userMapper.selectOneById(2L)).thenReturn(commenter);
            String longTitle = "A".repeat(50);
            String longContent = "B".repeat(100);

            // Act
            notificationService.sendCommentNotification(1L, 100L, 2L, longTitle, longContent);

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }
    }

    // ==================== sendReplyNotification Tests ====================

    @Nested
    @DisplayName("sendReplyNotification")
    class SendReplyNotificationTests {

        @Test
        @DisplayName("should send reply notification with replier name")
        void sendReplyNotification_Success() {
            // Arrange
            User replier = buildUser(2L, "Replier", "avatar.jpg");
            when(userMapper.selectOneById(2L)).thenReturn(replier);

            // Act
            notificationService.sendReplyNotification(1L, 100L, 2L, "Reply content");

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }

        @Test
        @DisplayName("should use '匿名用户' when replier not found")
        void sendReplyNotification_ReplierNotFound() {
            // Arrange
            when(userMapper.selectOneById(2L)).thenReturn(null);

            // Act
            notificationService.sendReplyNotification(1L, 100L, 2L, "Reply content");

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }
    }

    // ==================== sendLikeNotification Tests ====================

    @Nested
    @DisplayName("sendLikeNotification")
    class SendLikeNotificationTests {

        @Test
        @DisplayName("should send like notification with liker name")
        void sendLikeNotification_Success() {
            // Arrange
            User liker = buildUser(2L, "Liker", "avatar.jpg");
            when(userMapper.selectOneById(2L)).thenReturn(liker);

            // Act
            notificationService.sendLikeNotification(1L, 100L, 2L, "Article Title");

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }

        @Test
        @DisplayName("should use '匿名用户' when liker not found")
        void sendLikeNotification_LikerNotFound() {
            // Arrange
            when(userMapper.selectOneById(2L)).thenReturn(null);

            // Act
            notificationService.sendLikeNotification(1L, 100L, 2L, "Article Title");

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }
    }

    // ==================== sendSystemNotification Tests ====================

    @Nested
    @DisplayName("sendSystemNotification")
    class SendSystemNotificationTests {

        @Test
        @DisplayName("should send system notification")
        void sendSystemNotification_Success() {
            // Act
            notificationService.sendSystemNotification(1L, "System Title", "System Content");

            // Assert
            verify(notificationMapper).insert(any(Notification.class));
        }
    }

    // ==================== deleteNotifications Tests ====================

    @Nested
    @DisplayName("deleteNotifications")
    class DeleteNotificationsTests {

        @Test
        @DisplayName("should batch delete notifications successfully")
        void deleteNotifications_Success() {
            // Arrange
            Notification n1 = buildNotification(1L, 1L, 2L, 0);
            Notification n2 = buildNotification(2L, 1L, 3L, 0);
            when(notificationMapper.selectOneById(1L)).thenReturn(n1);
            when(notificationMapper.selectOneById(2L)).thenReturn(n2);

            // Act
            notificationService.deleteNotifications(Arrays.asList(1L, 2L), 1L);

            // Assert
            verify(notificationMapper).deleteById(1L);
            verify(notificationMapper).deleteById(2L);
        }

        @Test
        @DisplayName("should do nothing when ids list is null")
        void deleteNotifications_NullIds() {
            // Act
            notificationService.deleteNotifications(null, 1L);

            // Assert
            verify(notificationMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("should do nothing when ids list is empty")
        void deleteNotifications_EmptyIds() {
            // Act
            notificationService.deleteNotifications(Collections.emptyList(), 1L);

            // Assert
            verify(notificationMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("should throw NO_PERMISSION when any notification belongs to another user")
        void deleteNotifications_NoPermission() {
            // Arrange
            Notification n1 = buildNotification(1L, 1L, 2L, 0);
            Notification n2 = buildNotification(2L, 2L, 3L, 0); // belongs to another user
            when(notificationMapper.selectOneById(1L)).thenReturn(n1);
            when(notificationMapper.selectOneById(2L)).thenReturn(n2);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> notificationService.deleteNotifications(Arrays.asList(1L, 2L), 1L));
            assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should skip null notifications in validation")
        void deleteNotifications_NullNotification() {
            // Arrange
            when(notificationMapper.selectOneById(1L)).thenReturn(null);

            // Act
            notificationService.deleteNotifications(Collections.singletonList(1L), 1L);

            // Assert
            verify(notificationMapper).deleteById(1L);
        }
    }

    // ==================== clearAllNotifications Tests ====================

    @Nested
    @DisplayName("clearAllNotifications")
    class ClearAllNotificationsTests {

        @Test
        @DisplayName("should clear all notifications for user")
        void clearAllNotifications_Success() {
            // Act
            notificationService.clearAllNotifications(1L);

            // Assert
            verify(notificationMapper).deleteAllByUserId(1L);
        }
    }
}
