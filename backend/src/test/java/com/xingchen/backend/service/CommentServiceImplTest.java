package com.xingchen.backend.service;

import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.dto.CommentCreateDTO;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.Comment;
import com.xingchen.backend.entity.CommentLike;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.CommentLikeMapper;
import com.xingchen.backend.mapper.CommentMapper;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.impl.CommentServiceImpl;
import com.xingchen.backend.vo.CommentVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl Tests")
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentLikeMapper commentLikeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    // ==================== Helper Methods ====================

    private Article createArticle(Long id, Integer commentStatus, Integer isDeleted) {
        Article article = new Article();
        article.setId(id);
        article.setCommentStatus(commentStatus);
        article.setIsDeleted(isDeleted);
        article.setCommentNum(5);
        return article;
    }

    private Comment createComment(Long id, Long articleId, Long userId, Long rootId, Long parentId, Integer status) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setRootId(rootId);
        comment.setParentId(parentId);
        comment.setContent("test content");
        comment.setStatus(status);
        comment.setDeviceType("web");
        comment.setIsDeleted(0);
        return comment;
    }

    private CommentCreateDTO createDTO(Long articleId, String content, Long parentId, Long rootId, Long replyToId) {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(articleId);
        dto.setContent(content);
        dto.setParentId(parentId);
        dto.setRootId(rootId);
        dto.setReplyToId(replyToId);
        return dto;
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname("nickname_" + username);
        user.setAvatar("avatar.png");
        return user;
    }

    // ==================== createComment Tests ====================

    @Nested
    @DisplayName("createComment")
    class CreateCommentTests {

        @Test
        @DisplayName("should create top-level comment successfully")
        void createComment_Success() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;
            CommentCreateDTO dto = createDTO(articleId, "Great article!", null, null, null);
            Article article = createArticle(articleId, 1, 0);

            when(articleMapper.selectOneById(articleId)).thenReturn(article);
            // Simulate MyBatis-Flex auto-key fill: set ID on the entity during insert
            doAnswer(invocation -> {
                Comment c = invocation.getArgument(0);
                c.setId(1000L);
                return 1;
            }).when(commentMapper).insert(any(Comment.class));
            when(userMapper.selectOneById(userId)).thenReturn(createUser(userId, "testUser"));
            when(commentLikeMapper.selectLikedCommentIds(eq(userId), anyList())).thenReturn(new HashMap<>());

            // Act
            CommentVO result = commentService.createComment(userId, dto, "127.0.0.1", "web");

            // Assert
            assertNotNull(result);
            assertEquals(1000L, result.getId());
            assertEquals(articleId, result.getArticleId());
            assertEquals(userId, result.getUserId());
            assertEquals("Great article!", result.getContent());
            assertEquals(0L, result.getRootId());
            assertEquals(0L, result.getParentId());

            verify(articleMapper).selectOneById(articleId);
            verify(commentMapper).insert(any(Comment.class));
            verify(articleMapper).incrementCommentNum(articleId);
        }

        @Test
        @DisplayName("should create reply comment with parent correctly")
        void createComment_WithParent() {
            // Arrange
            Long userId = 2L;
            Long articleId = 100L;
            Long parentId = 500L;
            CommentCreateDTO dto = createDTO(articleId, "Nice reply!", parentId, null, null);
            Article article = createArticle(articleId, 1, 0);

            // Parent comment: rootId=0 means it is a root comment itself
            Comment parentComment = createComment(parentId, articleId, 3L, 0L, 0L, 1);

            when(articleMapper.selectOneById(articleId)).thenReturn(article);
            when(commentMapper.selectOneById(parentId)).thenReturn(parentComment);
            doAnswer(invocation -> {
                Comment c = invocation.getArgument(0);
                c.setId(1001L);
                return 1;
            }).when(commentMapper).insert(any(Comment.class));
            when(userMapper.selectOneById(userId)).thenReturn(createUser(userId, "replier"));
            when(commentLikeMapper.selectLikedCommentIds(eq(userId), anyList())).thenReturn(new HashMap<>());

            // Act
            CommentVO result = commentService.createComment(userId, dto, "192.168.1.1", "mobile");

            // Assert
            assertNotNull(result);
            assertEquals(1001L, result.getId());
            // rootId should be parentId since parentComment.rootId == 0
            assertEquals(parentId, result.getRootId());
            assertEquals(parentId, result.getParentId());

            verify(commentMapper).selectOneById(parentId);
            verify(articleMapper).incrementCommentNum(articleId);
        }

        @Test
        @DisplayName("should throw ARTICLE_NOT_FOUND when article does not exist")
        void createComment_ArticleNotFound() {
            // Arrange
            CommentCreateDTO dto = createDTO(999L, "content", null, null, null);
            when(articleMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentService.createComment(1L, dto, "127.0.0.1", "web"));
            assertEquals(ErrorCode.ARTICLE_NOT_FOUND.getCode(), ex.getCode());
            verify(commentMapper, never()).insert(any());
        }

        @Test
        @DisplayName("should throw ARTICLE_NOT_FOUND when article is soft-deleted")
        void createComment_ArticleDeleted() {
            // Arrange
            CommentCreateDTO dto = createDTO(100L, "content", null, null, null);
            Article deletedArticle = createArticle(100L, 1, 1);
            when(articleMapper.selectOneById(100L)).thenReturn(deletedArticle);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentService.createComment(1L, dto, "127.0.0.1", "web"));
            assertEquals(ErrorCode.ARTICLE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw COMMENT_DISABLED when article commentStatus is not 1")
        void createComment_CommentDisabled() {
            // Arrange
            Long articleId = 100L;
            CommentCreateDTO dto = createDTO(articleId, "content", null, null, null);
            Article article = createArticle(articleId, 0, 0); // commentStatus=0
            when(articleMapper.selectOneById(articleId)).thenReturn(article);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentService.createComment(1L, dto, "127.0.0.1", "web"));
            assertEquals(ErrorCode.COMMENT_DISABLED.getCode(), ex.getCode());
            verify(commentMapper, never()).insert(any());
        }

        @Test
        @DisplayName("should throw SYSTEM_ERROR when ID cannot be resolved")
        void createComment_IdNotResolved_ThrowsException() {
            // Arrange
            Long articleId = 100L;
            CommentCreateDTO dto = createDTO(articleId, "content", null, null, null);
            Article article = createArticle(articleId, 1, 0);

            when(articleMapper.selectOneById(articleId)).thenReturn(article);
            // insert does NOT set ID (simulates auto-key fill failure)
            when(commentMapper.insert(any(Comment.class))).thenReturn(1);
            // selectLastInsertId also returns null
            when(commentMapper.selectLastInsertId()).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentService.createComment(1L, dto, "127.0.0.1", "web"));
            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("评论创建失败"));
        }
    }

    // ==================== deleteComment Tests ====================

    @Nested
    @DisplayName("deleteComment")
    class DeleteCommentTests {

        @Test
        @DisplayName("should soft-delete comment successfully")
        void deleteComment_Success() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;
            Long articleId = 100L;
            Comment comment = createComment(commentId, articleId, userId, 0L, 0L, 1);

            when(commentMapper.selectOneById(commentId)).thenReturn(comment);
            when(commentMapper.update(any(Comment.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> commentService.deleteComment(userId, commentId));

            // Assert
            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper).update(captor.capture());
            assertEquals(2, captor.getValue().getStatus());
            verify(articleMapper).decrementCommentNum(articleId);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when comment does not exist")
        void deleteComment_NotFound() {
            // Arrange
            when(commentMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentService.deleteComment(1L, 999L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
            verify(commentMapper, never()).update(any());
        }

        @Test
        @DisplayName("should throw NOT_FOUND when comment is already deleted")
        void deleteComment_AlreadyDeleted() {
            // Arrange
            Comment deletedComment = createComment(500L, 100L, 1L, 0L, 0L, 2);
            deletedComment.setIsDeleted(1);
            when(commentMapper.selectOneById(500L)).thenReturn(deletedComment);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentService.deleteComment(1L, 500L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw COMMENT_DELETE_NO_PERMISSION when user is not the owner")
        void deleteComment_NoPermission() {
            // Arrange
            Long ownerId = 1L;
            Long otherUserId = 2L;
            Long commentId = 500L;
            Comment comment = createComment(commentId, 100L, ownerId, 0L, 0L, 1);

            when(commentMapper.selectOneById(commentId)).thenReturn(comment);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentService.deleteComment(otherUserId, commentId));
            assertEquals(ErrorCode.COMMENT_DELETE_NO_PERMISSION.getCode(), ex.getCode());
            verify(commentMapper, never()).update(any());
        }
    }

    // ==================== likeComment Tests ====================

    @Nested
    @DisplayName("likeComment")
    class LikeCommentTests {

        @Test
        @DisplayName("should like comment successfully")
        void likeComment_Success() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;

            when(commentLikeMapper.selectByCommentAndUser(commentId, userId)).thenReturn(null);
            when(commentLikeMapper.insert(any(CommentLike.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> commentService.likeComment(userId, commentId));

            // Assert
            verify(commentLikeMapper).insert(any(CommentLike.class));
            verify(commentMapper).incrementLikeNum(commentId);
        }

        @Test
        @DisplayName("should do nothing when user already liked the comment")
        void likeComment_AlreadyLiked() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;
            CommentLike existingLike = new CommentLike();
            existingLike.setCommentId(commentId);
            existingLike.setUserId(userId);

            when(commentLikeMapper.selectByCommentAndUser(commentId, userId)).thenReturn(existingLike);

            // Act
            assertDoesNotThrow(() -> commentService.likeComment(userId, commentId));

            // Assert - should not insert or increment
            verify(commentLikeMapper, never()).insert(any());
            verify(commentMapper, never()).incrementLikeNum(anyLong());
        }
    }

    // ==================== unlikeComment Tests ====================

    @Nested
    @DisplayName("unlikeComment")
    class UnlikeCommentTests {

        @Test
        @DisplayName("should unlike comment successfully")
        void unlikeComment_Success() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;

            when(commentLikeMapper.deleteByCommentAndUser(commentId, userId)).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> commentService.unlikeComment(userId, commentId));

            // Assert
            verify(commentLikeMapper).deleteByCommentAndUser(commentId, userId);
            verify(commentMapper).decrementLikeNum(commentId);
        }
    }
}
