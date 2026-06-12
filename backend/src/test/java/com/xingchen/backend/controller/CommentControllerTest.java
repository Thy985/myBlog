package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.CommentCreateDTO;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.Comment;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.service.CommentService;
import com.xingchen.backend.util.IpUtils;
import com.xingchen.backend.vo.CommentVO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController Tests")
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private CommentController commentController;

    private MockedStatic<StpUtil> stpUtilMock;
    private MockedStatic<IpUtils> ipUtilsMock;

    @BeforeEach
    void setUp() {
        commentController = new CommentController(commentService);

        // Mock StpUtil static methods
        stpUtilMock = mockStatic(StpUtil.class);

        // Mock IpUtils static methods
        ipUtilsMock = mockStatic(IpUtils.class);
        ipUtilsMock.when(() -> IpUtils.getClientIp(any(HttpServletRequest.class)))
                .thenReturn("127.0.0.1");
    }

    @AfterEach
    void tearDown() {
        if (stpUtilMock != null) {
            stpUtilMock.close();
        }
        if (ipUtilsMock != null) {
            ipUtilsMock.close();
        }
    }

    // ==================== Helper Methods ====================

    private CommentVO createCommentVO(Long id, Long articleId, Long userId, String content) {
        CommentVO vo = new CommentVO();
        vo.setId(id);
        vo.setArticleId(articleId);
        vo.setUserId(userId);
        vo.setRootId(0L);
        vo.setParentId(0L);
        vo.setContent(content);
        vo.setLikeCount(0);
        vo.setReplyCount(0);
        vo.setStatus("pending");
        vo.setDevice("web");
        vo.setCreatedTime(LocalDateTime.now());
        vo.setUsername("user" + userId);
        vo.setNickname("User " + userId);
        vo.setAvatar("avatar.png");
        vo.setIsLiked(false);
        return vo;
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

    private MockHttpServletRequest buildRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 TestAgent");
        return request;
    }

    // ==================== createComment Tests ====================

    @Nested
    @DisplayName("POST /api/comment - 创建评论")
    class CreateCommentTests {

        @Test
        @DisplayName("应该创建顶级评论成功")
        void createComment_TopLevelComment_Success() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            Long userId = 1L;
            Long articleId = 100L;
            CommentCreateDTO dto = createDTO(articleId, "Great article!", null, null, null);

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);

            CommentVO createdVO = createCommentVO(1001L, articleId, userId, "Great article!");
            when(commentService.createComment(eq(userId), eq(dto), eq("127.0.0.1"), eq("Mozilla/5.0 TestAgent")))
                    .thenReturn(createdVO);

            // Act
            Result<CommentVO> result = commentController.createComment(dto, request);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertEquals(1001L, result.getData().getId());
            assertEquals(articleId, result.getData().getArticleId());
            assertEquals(userId, result.getData().getUserId());
            assertEquals("Great article!", result.getData().getContent());

            verify(commentService).createComment(eq(userId), eq(dto), eq("127.0.0.1"), eq("Mozilla/5.0 TestAgent"));
        }

        @Test
        @DisplayName("应该创建回复评论成功")
        void createComment_ReplyComment_Success() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            Long userId = 2L;
            Long articleId = 100L;
            Long parentId = 500L;
            CommentCreateDTO dto = createDTO(articleId, "Nice reply!", parentId, null, 3L);

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);

            CommentVO replyVO = createCommentVO(1002L, articleId, userId, "Nice reply!");
            replyVO.setParentId(parentId);
            replyVO.setRootId(500L);
            when(commentService.createComment(eq(userId), eq(dto), anyString(), anyString()))
                    .thenReturn(replyVO);

            // Act
            Result<CommentVO> result = commentController.createComment(dto, request);

            // Assert
            assertEquals(200, result.getCode());
            assertEquals(parentId, result.getData().getParentId());
            assertEquals(500L, result.getData().getRootId());
            verify(commentService).createComment(eq(userId), eq(dto), anyString(), anyString());
        }

        @Test
        @DisplayName("应该在服务抛出文章不存在异常时向上传播")
        void createComment_ServiceThrowsArticleNotFoundException_ShouldPropagate() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            Long userId = 1L;
            CommentCreateDTO dto = createDTO(999L, "content", null, null, null);

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(commentService.createComment(anyLong(), any(CommentCreateDTO.class), anyString(), anyString()))
                    .thenThrow(new BusinessException(ErrorCode.ARTICLE_NOT_FOUND.getCode(),
                            ErrorCode.ARTICLE_NOT_FOUND.getMessage()));

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentController.createComment(dto, request));
            assertEquals(ErrorCode.ARTICLE_NOT_FOUND.getCode(), ex.getCode());
            verify(commentService).createComment(anyLong(), any(CommentCreateDTO.class), anyString(), anyString());
        }

        @Test
        @DisplayName("应该在服务抛出评论已关闭异常时向上传播")
        void createComment_ServiceThrowsCommentDisabledException_ShouldPropagate() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            Long userId = 1L;
            CommentCreateDTO dto = createDTO(100L, "content", null, null, null);

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(commentService.createComment(anyLong(), any(CommentCreateDTO.class), anyString(), anyString()))
                    .thenThrow(new BusinessException(ErrorCode.COMMENT_DISABLED.getCode(),
                            ErrorCode.COMMENT_DISABLED.getMessage()));

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentController.createComment(dto, request));
            assertEquals(ErrorCode.COMMENT_DISABLED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("应该在服务抛出系统异常时向上传播")
        void createComment_ServiceThrowsSystemException_ShouldPropagate() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            Long userId = 1L;
            CommentCreateDTO dto = createDTO(100L, "content", null, null, null);

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(commentService.createComment(anyLong(), any(CommentCreateDTO.class), anyString(), anyString()))
                    .thenThrow(new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                            ErrorCode.SYSTEM_ERROR.getMessage()));

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentController.createComment(dto, request));
            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), ex.getCode());
        }
    }

    // ==================== getCommentTreeByArticleId Tests ====================

    @Nested
    @DisplayName("GET /api/comment/list - 获取文章评论树")
    class GetCommentTreeTests {

        @Test
        @DisplayName("应该在已登录时返回评论树并携带点赞状态")
        void getCommentTree_WhenLoggedIn_ShouldReturnTreeWithLikeStatus() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);

            List<CommentVO> commentTree = List.of(
                    createCommentVO(1001L, articleId, 2L, "Great article!"),
                    createCommentVO(1002L, articleId, 3L, "Thanks!")
            );
            commentTree.get(0).setIsLiked(true);

            when(commentService.getCommentTreeByArticleId(articleId, userId)).thenReturn(commentTree);

            // Act
            Result<List<CommentVO>> result = commentController.getCommentTreeByArticleId(articleId);

            // Assert
            assertEquals(200, result.getCode());
            assertEquals(2, result.getData().size());
            assertTrue(result.getData().get(0).getIsLiked());
            assertFalse(result.getData().get(1).getIsLiked());
            verify(commentService).getCommentTreeByArticleId(articleId, userId);
        }

        @Test
        @DisplayName("应该在未登录时返回评论树且点赞状态为空")
        void getCommentTree_WhenNotLoggedIn_ShouldReturnTreeWithNullLikeStatus() {
            // Arrange
            Long articleId = 100L;

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

            List<CommentVO> commentTree = List.of(
                    createCommentVO(1001L, articleId, 2L, "Good post!")
            );
            when(commentService.getCommentTreeByArticleId(articleId, null)).thenReturn(commentTree);

            // Act
            Result<List<CommentVO>> result = commentController.getCommentTreeByArticleId(articleId);

            // Assert
            assertEquals(200, result.getCode());
            assertEquals(1, result.getData().size());
            assertNull(result.getData().get(0).getIsLiked());
            verify(commentService).getCommentTreeByArticleId(articleId, null);
        }

        @Test
        @DisplayName("应该在没有评论时返回空列表")
        void getCommentTree_NoComments_ShouldReturnEmptyList() {
            // Arrange
            Long articleId = 999L;

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
            when(commentService.getCommentTreeByArticleId(articleId, null)).thenReturn(List.of());

            // Act
            Result<List<CommentVO>> result = commentController.getCommentTreeByArticleId(articleId);

            // Assert
            assertEquals(200, result.getCode());
            assertTrue(result.getData().isEmpty());
        }
    }

    // ==================== getRepliesByRootId Tests ====================

    @Nested
    @DisplayName("GET /api/comment/{rootId}/replies - 获取根评论的回复列表")
    class GetRepliesTests {

        @Test
        @DisplayName("应该在已登录时返回回复列表并携带点赞状态")
        void getReplies_WhenLoggedIn_ShouldReturnRepliesWithLikeStatus() {
            // Arrange
            Long userId = 1L;
            Long rootId = 500L;

            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);

            List<CommentVO> replies = List.of(
                    createCommentVO(1001L, 100L, 2L, "I agree!"),
                    createCommentVO(1002L, 100L, 3L, "Me too!")
            );
            replies.get(0).setIsLiked(true);
            replies.get(1).setIsLiked(false);

            when(commentService.getRepliesByRootId(rootId, userId)).thenReturn(replies);

            // Act
            Result<List<CommentVO>> result = commentController.getRepliesByRootId(rootId);

            // Assert
            assertEquals(200, result.getCode());
            assertEquals(2, result.getData().size());
            assertTrue(result.getData().get(0).getIsLiked());
            verify(commentService).getRepliesByRootId(rootId, userId);
        }

        @Test
        @DisplayName("应该在未登录时返回回复列表且点赞状态为空")
        void getReplies_WhenNotLoggedIn_ShouldReturnRepliesWithNullLikeStatus() {
            // Arrange
            Long rootId = 500L;

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

            List<CommentVO> replies = List.of(
                    createCommentVO(1001L, 100L, 2L, "Reply content")
            );
            when(commentService.getRepliesByRootId(rootId, null)).thenReturn(replies);

            // Act
            Result<List<CommentVO>> result = commentController.getRepliesByRootId(rootId);

            // Assert
            assertEquals(200, result.getCode());
            assertNull(result.getData().get(0).getIsLiked());
            verify(commentService).getRepliesByRootId(rootId, null);
        }
    }

    // ==================== deleteComment Tests ====================

    @Nested
    @DisplayName("DELETE /api/comment/{id} - 删除评论")
    class DeleteCommentTests {

        @Test
        @DisplayName("应该删除自己的评论成功")
        void deleteComment_OwnComment_ShouldSucceed() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doNothing().when(commentService).deleteComment(userId, commentId);

            // Act
            Result<Void> result = commentController.deleteComment(commentId);

            // Assert
            assertEquals(200, result.getCode());
            verify(commentService).deleteComment(userId, commentId);
        }

        @Test
        @DisplayName("应该在服务抛出无权限异常时向上传播")
        void deleteComment_NoPermission_ShouldPropagateException() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doThrow(new BusinessException(ErrorCode.COMMENT_DELETE_NO_PERMISSION.getCode(),
                            ErrorCode.COMMENT_DELETE_NO_PERMISSION.getMessage()))
                    .when(commentService).deleteComment(userId, commentId);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentController.deleteComment(commentId));
            assertEquals(ErrorCode.COMMENT_DELETE_NO_PERMISSION.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("无权限删除该评论"));
            verify(commentService).deleteComment(userId, commentId);
        }

        @Test
        @DisplayName("应该在评论不存在时抛出异常")
        void deleteComment_CommentNotFound_ShouldPropagateException() {
            // Arrange
            Long userId = 1L;
            Long commentId = 999L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doThrow(new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                            ErrorCode.NOT_FOUND.getMessage()))
                    .when(commentService).deleteComment(userId, commentId);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentController.deleteComment(commentId));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
            verify(commentService).deleteComment(userId, commentId);
        }

        @Test
        @DisplayName("应该在评论已被删除时抛出异常")
        void deleteComment_AlreadyDeleted_ShouldPropagateException() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doThrow(new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                            ErrorCode.NOT_FOUND.getMessage()))
                    .when(commentService).deleteComment(userId, commentId);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentController.deleteComment(commentId));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }

    // ==================== likeComment Tests ====================

    @Nested
    @DisplayName("POST /api/comment/{id}/like - 点赞评论")
    class LikeCommentTests {

        @Test
        @DisplayName("应该点赞评论成功")
        void likeComment_Success() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doNothing().when(commentService).likeComment(userId, commentId);

            // Act
            Result<Void> result = commentController.likeComment(commentId);

            // Assert
            assertEquals(200, result.getCode());
            verify(commentService).likeComment(userId, commentId);
        }

        @Test
        @DisplayName("应该在重复点赞时静默成功")
        void likeComment_AlreadyLiked_ShouldSucceedSilently() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doNothing().when(commentService).likeComment(userId, commentId);

            // Act
            Result<Void> result = commentController.likeComment(commentId);

            // Assert
            assertEquals(200, result.getCode());
            verify(commentService).likeComment(userId, commentId);
        }
    }

    // ==================== unlikeComment Tests ====================

    @Nested
    @DisplayName("DELETE /api/comment/{id}/like - 取消点赞评论")
    class UnlikeCommentTests {

        @Test
        @DisplayName("应该取消点赞评论成功")
        void unlikeComment_Success() {
            // Arrange
            Long userId = 1L;
            Long commentId = 500L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doNothing().when(commentService).unlikeComment(userId, commentId);

            // Act
            Result<Void> result = commentController.unlikeComment(commentId);

            // Assert
            assertEquals(200, result.getCode());
            verify(commentService).unlikeComment(userId, commentId);
        }
    }

    // ==================== getPendingComments Tests ====================

    @Nested
    @DisplayName("GET /api/comment/pending - 获取待审核评论")
    class GetPendingCommentsTests {

        @Test
        @DisplayName("应该返回待审核评论列表")
        void getPendingComments_Success() {
            // Arrange
            int page = 1;
            int size = 20;

            List<CommentVO> pendingComments = List.of(
                    createCommentVO(2001L, 100L, 2L, "Pending comment 1"),
                    createCommentVO(2002L, 101L, 3L, "Pending comment 2")
            );
            pendingComments.forEach(c -> c.setStatus("pending"));

            when(commentService.getPendingComments(page, size)).thenReturn(pendingComments);

            // Act
            Result<PageResult<CommentVO>> result = commentController.getPendingComments(page, size);

            // Assert
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertEquals(2, result.getData().getList().size());
            assertEquals("pending", result.getData().getList().get(0).getStatus());
            assertEquals(page, result.getData().getPage());
            assertEquals(size, result.getData().getSize());
            verify(commentService).getPendingComments(page, size);
        }

        @Test
        @DisplayName("应该在无待审核评论时返回空列表")
        void getPendingComments_NoPendingComments_ShouldReturnEmpty() {
            // Arrange
            int page = 1;
            int size = 20;

            when(commentService.getPendingComments(page, size)).thenReturn(List.of());

            // Act
            Result<PageResult<CommentVO>> result = commentController.getPendingComments(page, size);

            // Assert
            assertEquals(200, result.getCode());
            assertTrue(result.getData().getList().isEmpty());
            assertEquals(0L, result.getData().getTotal());
        }

        @Test
        @DisplayName("应该使用默认参数获取待审核评论")
        void getPendingComments_WithDefaultParams() {
            // Arrange
            when(commentService.getPendingComments(1, 20)).thenReturn(List.of());

            // Act
            Result<PageResult<CommentVO>> result = commentController.getPendingComments(1, 20);

            // Assert
            assertEquals(200, result.getCode());
            verify(commentService).getPendingComments(1, 20);
        }
    }

    // ==================== approveComment Tests ====================

    @Nested
    @DisplayName("PUT /api/comment/{id}/approve - 审核通过评论")
    class ApproveCommentTests {

        @Test
        @DisplayName("应该审核通过评论成功")
        void approveComment_Success() {
            // Arrange
            Long commentId = 500L;
            doNothing().when(commentService).approveComment(commentId);

            // Act
            Result<Void> result = commentController.approveComment(commentId);

            // Assert
            assertEquals(200, result.getCode());
            verify(commentService).approveComment(commentId);
        }
    }

    // ==================== rejectComment Tests ====================

    @Nested
    @DisplayName("PUT /api/comment/{id}/reject - 拒绝评论")
    class RejectCommentTests {

        @Test
        @DisplayName("应该拒绝评论成功")
        void rejectComment_Success() {
            // Arrange
            Long commentId = 500L;
            doNothing().when(commentService).rejectComment(commentId);

            // Act
            Result<Void> result = commentController.rejectComment(commentId);

            // Assert
            assertEquals(200, result.getCode());
            verify(commentService).rejectComment(commentId);
        }
    }

    // ==================== getUserComments Tests ====================

    @Nested
    @DisplayName("GET /api/comment/user - 获取当前用户评论")
    class GetUserCommentsTests {

        @Test
        @DisplayName("应该返回当前用户的评论列表")
        void getUserComments_Success() {
            // Arrange
            Long userId = 1L;
            int page = 1;
            int size = 10;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);

            List<CommentVO> userComments = List.of(
                    createCommentVO(1001L, 100L, userId, "My comment 1"),
                    createCommentVO(1002L, 101L, userId, "My comment 2")
            );
            PageResult<CommentVO> pageResult = PageResult.of(userComments, 2L, page, size);

            when(commentService.getUserComments(userId, page, size)).thenReturn(pageResult);

            // Act
            Result<PageResult<CommentVO>> result = commentController.getUserComments(page, size);

            // Assert
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertEquals(2, result.getData().getList().size());
            assertEquals(userId, result.getData().getList().get(0).getUserId());
            assertEquals(2L, result.getData().getTotal());
            assertEquals(page, result.getData().getPage());
            verify(commentService).getUserComments(userId, page, size);
        }

        @Test
        @DisplayName("应该在没有评论时返回空分页结果")
        void getUserComments_NoComments_ShouldReturnEmpty() {
            // Arrange
            Long userId = 1L;
            int page = 1;
            int size = 10;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);

            PageResult<CommentVO> emptyResult = PageResult.of(List.of(), 0L, page, size);
            when(commentService.getUserComments(userId, page, size)).thenReturn(emptyResult);

            // Act
            Result<PageResult<CommentVO>> result = commentController.getUserComments(page, size);

            // Assert
            assertEquals(200, result.getCode());
            assertTrue(result.getData().getList().isEmpty());
            assertEquals(0L, result.getData().getTotal());
        }

        @Test
        @DisplayName("应该在用户有跨多页评论时正确返回分页信息")
        void getUserComments_MultiplePages_ShouldReturnCorrectPagination() {
            // Arrange
            Long userId = 1L;
            int page = 2;
            int size = 10;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);

            List<CommentVO> pageComments = List.of(
                    createCommentVO(1011L, 100L, userId, "Comment 11"),
                    createCommentVO(1012L, 101L, userId, "Comment 12")
            );
            PageResult<CommentVO> pageResult = PageResult.of(pageComments, 25L, page, size);

            when(commentService.getUserComments(userId, page, size)).thenReturn(pageResult);

            // Act
            Result<PageResult<CommentVO>> result = commentController.getUserComments(page, size);

            // Assert
            assertEquals(200, result.getCode());
            assertEquals(25L, result.getData().getTotal());
            assertEquals(page, result.getData().getPage());
            assertEquals(size, result.getData().getSize());
        }
    }
}
