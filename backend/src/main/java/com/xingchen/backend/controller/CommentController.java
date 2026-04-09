package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.CommentCreateDTO;
import com.xingchen.backend.service.CommentService;
import com.xingchen.backend.util.IpUtils;
import com.xingchen.backend.vo.CommentVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
@Validated
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/list")
    public Result<List<CommentVO>> getCommentTreeByArticleId(@RequestParam Long articleId) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return Result.success(commentService.getCommentTreeByArticleId(articleId, userId));
    }

    @GetMapping("/{rootId}/replies")
    public Result<List<CommentVO>> getRepliesByRootId(@PathVariable Long rootId) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return Result.success(commentService.getRepliesByRootId(rootId, userId));
    }

    @SaCheckLogin
    @PostMapping
    public Result<CommentVO> createComment(@Valid @RequestBody CommentCreateDTO dto,
                                           HttpServletRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String ip = IpUtils.getClientIp(request);
        String device = request.getHeader("User-Agent");
        return Result.success(commentService.createComment(userId, dto, ip, device));
    }

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        commentService.deleteComment(userId, id);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/{id}/like")
    public Result<Void> likeComment(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        commentService.likeComment(userId, id);
        return Result.success();
    }

    @SaCheckLogin
    @DeleteMapping("/{id}/like")
    public Result<Void> unlikeComment(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        commentService.unlikeComment(userId, id);
        return Result.success();
    }

    @GetMapping("/pending")
    @SaCheckRole("ADMIN")
    public Result<PageResult<CommentVO>> getPendingComments(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        List<CommentVO> list = commentService.getPendingComments(page, size);
        return Result.success(PageResult.of(list, (long) list.size(), page, size));
    }

    @PutMapping("/{id}/approve")
    @SaCheckRole("ADMIN")
    public Result<Void> approveComment(@PathVariable Long id) {
        commentService.approveComment(id);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @SaCheckRole("ADMIN")
    public Result<Void> rejectComment(@PathVariable Long id) {
        commentService.rejectComment(id);
        return Result.success();
    }

    @SaCheckLogin
    @GetMapping("/user")
    public Result<PageResult<CommentVO>> getUserComments(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(commentService.getUserComments(userId, page, size));
    }
}
