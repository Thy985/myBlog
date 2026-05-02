package com.xingchen.backend.service.impl;

import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.dto.CommentCreateDTO;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.Comment;
import com.xingchen.backend.entity.CommentLike;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.CommentLikeMapper;
import com.xingchen.backend.mapper.CommentMapper;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.CommentService;
import com.xingchen.backend.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;

    @Override
    public List<CommentVO> getCommentTreeByArticleId(Long articleId, Long userId) {
        List<Comment> allComments = commentMapper.selectByArticleId(articleId);
        return buildCommentTree(allComments, userId);
    }

    @Override
    public List<CommentVO> getRepliesByRootId(Long rootId, Long userId) {
        List<Comment> replies = commentMapper.selectRepliesByRootId(rootId);
        return replies.stream()
                .map(c -> convertToVO(c, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public List<CommentVO> getArticleComments(Long articleId) {
        List<Comment> comments = commentMapper.selectByArticleId(articleId);
        return comments.stream()
                .map(c -> convertToVO(c, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public CommentVO createComment(Long userId, CommentCreateDTO dto, String ip, String device) {
        Article article = articleMapper.selectOneById(dto.getArticleId());
        if (article == null || java.util.Objects.equals(1, article.getIsDeleted())) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }

        if (!java.util.Objects.equals(1, article.getCommentStatus())) {
            throw new BusinessException(ErrorCode.COMMENT_DISABLED);
        }

        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        
        Long parentId = dto.getParentId();
        if (parentId != null && parentId > 0) {
            Comment parentComment = commentMapper.selectOneById(parentId);
            if (parentComment != null) {
                // 优先使用前端传递的 rootId，否则自动计算
                Long rootId = dto.getRootId();
                if (rootId == null || rootId == 0) {
                    rootId = parentComment.getRootId() == 0 ? parentId : parentComment.getRootId();
                }
                comment.setRootId(rootId);
                comment.setParentId(parentId);
                // 设置回复用户ID
                if (dto.getReplyToId() != null && dto.getReplyToId() > 0) {
                    comment.setReplyToUserId(dto.getReplyToId());
                } else {
                    comment.setReplyToUserId(parentComment.getUserId());
                }
            } else {
                comment.setRootId(0L);
                comment.setParentId(0L);
            }
        } else {
            comment.setRootId(0L);
            comment.setParentId(0L);
        }
        
        comment.setStatus(1);
        comment.setDeviceType(device);
        comment.setCreateTime(LocalDateTime.now());

        commentMapper.insert(comment);

        // MyBatis-Flex Auto Key 可能未回填，使用 LAST_INSERT_ID
        if (comment.getId() == null) {
            Long lastId = commentMapper.selectLastInsertId();
            if (lastId != null && lastId > 0) {
                comment.setId(lastId);
            }
        }

        // 仍然无法获取 ID，直接返回错误
        if (comment.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论创建失败：无法获取评论ID");
        }

        articleMapper.incrementCommentNum(dto.getArticleId());

        log.info("用户 {} 评论成功，评论ID: {}", userId, comment.getId());

        return convertToVO(comment, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public void deleteComment(Long userId, Long id) {
        Comment comment = commentMapper.selectOneById(id);
        if (comment == null || java.util.Objects.equals(1, comment.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_DELETE_NO_PERMISSION);
        }
        comment.setStatus(2);
        commentMapper.update(comment);
        articleMapper.decrementCommentNum(comment.getArticleId());
        log.info("用户 {} 删除评论成功，评论ID: {}", userId, id);
    }

    @Override
    @Transactional
    public void likeComment(Long userId, Long id) {
        CommentLike existing = commentLikeMapper.selectByCommentAndUser(id, userId);
        if (existing != null) {
            return;
        }

        CommentLike like = new CommentLike();
        like.setCommentId(id);
        like.setUserId(userId);
        commentLikeMapper.insert(like);
        commentMapper.incrementLikeNum(id);
    }

    @Override
    @Transactional
    public void unlikeComment(Long userId, Long id) {
        commentLikeMapper.deleteByCommentAndUser(id, userId);
        commentMapper.decrementLikeNum(id);
    }

    @Override
    public List<CommentVO> getPendingComments(Integer page, Integer size) {
        List<Comment> comments = commentMapper.selectPendingComments();
        return comments.stream()
                .map(c -> convertToVO(c, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveComment(Long id) {
        Comment comment = commentMapper.selectOneById(id);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        comment.setStatus(1);
        comment.setAuditTime(LocalDateTime.now());
        commentMapper.update(comment);
    }

    @Override
    @Transactional
    public void rejectComment(Long id) {
        Comment comment = commentMapper.selectOneById(id);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        comment.setStatus(3);
        comment.setAuditTime(LocalDateTime.now());
        commentMapper.update(comment);
    }

    @Override
    public PageResult<CommentVO> getUserComments(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Comment> comments = commentMapper.selectByUserId(userId, offset, size);
        long total = commentMapper.countByUserId(userId);
        List<CommentVO> voList = comments.stream()
                .map(c -> convertToVO(c, userId))
                .collect(Collectors.toList());
        int pages = (int) Math.ceil((double) total / size);
        return PageResult.of(voList, total, pages, page, size);
    }

    private List<CommentVO> buildCommentTree(List<Comment> comments, Long userId) {
        if (comments == null || comments.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, CommentVO> voMap = new LinkedHashMap<>();
        List<CommentVO> rootComments = new ArrayList<>();

        // 优化N+1查询：预先批量查询所有用户
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 优化N+1查询：预先批量查询点赞状态
        List<Long> commentIds = comments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
        Map<Long, Boolean> likeMap = commentLikeMapper.selectLikedCommentIds(userId, commentIds);

        for (Comment comment : comments) {
            CommentVO vo = convertToVO(comment, userId, userMap, likeMap);
            voMap.put(comment.getId(), vo);
        }

        for (Comment comment : comments) {
            CommentVO vo = voMap.get(comment.getId());
            if (comment.getRootId() == 0) {
                rootComments.add(vo);
            } else {
                CommentVO parentVO = voMap.get(comment.getRootId());
                if (parentVO != null) {
                    if (parentVO.getReplies() == null) {
                        parentVO.setReplies(new ArrayList<>());
                    }
                    parentVO.getReplies().add(vo);
                }
            }
        }

        return rootComments;
    }

    /**
     * 优化后的convertToVO，使用预查询的数据
     */
    private CommentVO convertToVO(Comment comment, Long currentUserId,
                                   Map<Long, User> userMap, Map<Long, Boolean> likeMap) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setRootId(comment.getRootId());
        vo.setParentId(comment.getParentId());
        vo.setStatus(String.valueOf(comment.getStatus()));
        vo.setDevice(comment.getDeviceType());
        vo.setCreatedTime(comment.getCreateTime());
        vo.setLikeCount(comment.getLikeCount());
        vo.setReplyCount(comment.getReplyCount());

        // 使用预查询的用户数据
        User user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        // 使用预查询的点赞状态
        if (currentUserId != null) {
            vo.setIsLiked(Boolean.TRUE.equals(likeMap.get(comment.getId())));
        }

        return vo;
    }

    /**
     * 原来的convertToVO，保持兼容性（用于单条转换场景）
     */
    private CommentVO convertToVO(Comment comment, Long currentUserId) {
        User user = userMapper.selectOneById(comment.getUserId());
        Map<Long, User> userMap = new HashMap<>();
        if (user != null) {
            userMap.put(user.getId(), user);
        }
        Map<Long, Boolean> likeMap = currentUserId != null
                ? commentLikeMapper.selectLikedCommentIds(currentUserId, List.of(comment.getId()))
                : new HashMap<>();
        return convertToVO(comment, currentUserId, userMap, likeMap);
    }
}
