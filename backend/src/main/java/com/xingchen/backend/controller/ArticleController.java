package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.aspect.RateLimit;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.dto.ArticleUpdateDTO;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.SearchService;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.ArticleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
@Validated
public class ArticleController {

    private final ArticleService articleService;
    private final SearchService searchService;

    @SaCheckLogin
    @PostMapping
    public Result<ArticleVO> createArticle(@Valid @RequestBody ArticleCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(articleService.createArticle(userId, dto));
    }

    @SaCheckLogin
    @PutMapping("/{id}")
    public Result<ArticleVO> updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(articleService.updateArticle(userId, id, dto));
    }

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        articleService.deleteArticle(userId, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> getArticle(@PathVariable Long id) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return Result.success(articleService.getArticleById(id, userId));
    }

    @GetMapping("/list")
    public Result<PageResult<ArticleListVO>> getArticleList(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId) {
        return Result.success(articleService.getArticleList(page, size, keyword, categoryId, tagId));
    }

    @GetMapping("/user/{userId}")
    public Result<PageResult<ArticleListVO>> getUserArticles(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer size) {
        return Result.success(articleService.getUserArticles(userId, page, size));
    }

    @GetMapping("/hot")
    public Result<List<ArticleVO>> getHotArticles(@RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer limit) {
        return Result.success(articleService.getHotArticles(limit));
    }

    @SaCheckLogin
    @PostMapping("/{id}/publish")
    public Result<Void> publishArticle(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        articleService.publishArticle(userId, id);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/{id}/offline")
    @RateLimit(perMinute = 10, message = "下线操作过于频繁")
    public Result<Void> offlineArticle(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        articleService.offlineArticle(userId, id);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/{id}/top")
    public Result<Void> topArticle(@PathVariable Long id, @RequestParam Boolean isTop) {
        Long userId = StpUtil.getLoginIdAsLong();
        articleService.topArticle(userId, id, isTop);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/{id}/like")
    public Result<Void> likeArticle(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        articleService.likeArticle(userId, id);
        return Result.success();
    }

    @SaCheckLogin
    @DeleteMapping("/{id}/like")
    public Result<Void> unlikeArticle(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        articleService.unlikeArticle(userId, id);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/{id}/collect")
    public Result<Void> collectArticle(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        articleService.collectArticle(userId, id);
        return Result.success();
    }

    @SaCheckLogin
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollectArticle(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        articleService.uncollectArticle(userId, id);
        return Result.success();
    }

    @SaCheckLogin
    @GetMapping("/user/collects")
    public Result<PageResult<ArticleListVO>> getUserCollects(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(articleService.getUserCollects(userId, page, size));
    }

    @GetMapping("/archive")
    public Result<Map<String, Object>> getArticleArchive(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) Integer size) {
        return Result.success(articleService.getArticleArchive(page, size));
    }

    @SaCheckLogin
    @GetMapping("/user/archive")
    public Result<Map<String, Object>> getUserArticleArchive(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(articleService.getUserArticleArchive(userId, page, size));
    }

    @SaCheckLogin
    @PostMapping("/{id}/read")
    public Result<Void> updateReadNum(@PathVariable Long id) {
        articleService.updateReadNum(id);
        return Result.success();
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> getArticleReadStats(@PathVariable Long id) {
        return Result.success(articleService.getArticleReadStats(id));
    }

    @GetMapping("/related")
    public Result<List<ArticleListVO>> getRelatedArticles(
            @RequestParam Long articleId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) Integer limit) {
        return Result.success(articleService.getRelatedArticles(articleId, limit));
    }

    @GetMapping("/search")
    public Result<PageResult<ArticleListVO>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer size) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        PageResult<ArticleListVO> result = articleService.getUserArticles(userId, page, size, keyword);
        searchService.saveSearchHistoryAsync(userId, keyword, result.getList() != null ? result.getList().size() : 0);
        return Result.success(result);
    }
}
