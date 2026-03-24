package com.xingchen.backend.service;

import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.dto.CommentCreateDTO;
import com.xingchen.backend.vo.CommentVO;

import java.util.List;

public interface CommentService {
    List<CommentVO> getCommentTreeByArticleId(Long articleId, Long userId);

    List<CommentVO> getRepliesByRootId(Long rootId, Long userId);

    @Deprecated
    List<CommentVO> getArticleComments(Long articleId);

    CommentVO createComment(Long userId, CommentCreateDTO dto, String ip, String device);

    void deleteComment(Long userId, Long id);

    void likeComment(Long userId, Long id);

    void unlikeComment(Long userId, Long id);

    List<CommentVO> getPendingComments(Integer page, Integer size);

    void approveComment(Long id);

    void rejectComment(Long id);

    PageResult<CommentVO> getUserComments(Long userId, Integer page, Integer size);
}
