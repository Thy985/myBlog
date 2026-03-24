package com.xingchen.backend.controller;

import com.xingchen.backend.common.Result;
import com.xingchen.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取核心统计数据
     */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> getOverviewStats() {
        Map<String, Object> stats = dashboardService.getOverviewStats();
        return Result.success(stats);
    }
    /**
     * 获取文章发布趋势
     * @param days 时间范围，默认7天
     * @return 文章发布趋势数据
     */
    @GetMapping("/stats/article-trend")
    public Result<Map<String, Object>> getArticleTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        Map<String, Object> trend = dashboardService.getArticleTrend(days);
        return Result.success(trend);
    }
    /**
     * 获取用户增长趋势
     * @param days 时间范围，默认7天
     * @return 用户增长趋势数据
     */
    @GetMapping("/stats/user-trend")
    public Result<Map<String, Object>> getUserTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        Map<String, Object> trend = dashboardService.getUserTrend(days);
        return Result.success(trend);
    }

    /**
     * 获取访问量趋势
     * @param days 时间范围，默认7天
     * @return 访问量趋势数据
     */
    @GetMapping("/stats/visit-trend")
    public Result<Map<String, Object>> getVisitTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        Map<String, Object> trend = dashboardService.getVisitTrend(days);
        return Result.success(trend);
    }
    /**
     * 获取分类分布统计
     * @return 分类分布数据
     * 包含每个分类的文章数量和占比
     */
    @GetMapping("/stats/category-distribution")
    public Result<List<Map<String, Object>>> getCategoryDistribution() {
        List<Map<String, Object>> distribution = dashboardService.getCategoryDistribution();
        return Result.success(distribution);
    }
    /**
     * 获取标签分布统计
     * @return 标签分布数据
     * 包含每个标签的文章数量和占比
     */
    @GetMapping("/stats/tag-distribution")
    public Result<List<Map<String, Object>>> getTagDistribution() {
        List<Map<String, Object>> distribution = dashboardService.getTagDistribution();
        return Result.success(distribution);
    }
    /**
     * 获取热门文章排行
     * @param limit 返回文章数量，默认10条
     * @return 热门文章列表
     */
    @GetMapping("/stats/hot-articles")
    public Result<List<Map<String, Object>>> getHotArticles(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Map<String, Object>> articles = dashboardService.getHotArticles(limit);
        return Result.success(articles);
    }
    /**
     * 获取活跃用户排行
     * @param limit 返回用户数量，默认10条
     * @return 活跃用户列表
     * 包含每个用户的文章数量和占比
     */
    @GetMapping("/stats/active-users")
    public Result<List<Map<String, Object>>> getActiveUsers(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Map<String, Object>> users = dashboardService.getActiveUsers(limit);
        return Result.success(users);
    }
    /**
     * 获取待审核内容统计
     * @return 待审核内容统计数据
     * 包含文章、评论、用户待审核数量
     */
    @GetMapping("/stats/pending-review")
    public Result<Map<String, Integer>> getPendingReviewStats() {
        Map<String, Integer> stats = dashboardService.getPendingReviewStats();
        return Result.success(stats);
    }

    /**
     * 获取系统运行状态
     * @return 系统状态数据
     * 包含数据库连接状态、缓存状态、队列状态等
     */
    @GetMapping("/stats/system-status")
    public Result<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = dashboardService.getSystemStatus();
        return Result.success(status);
    }
    /**
     * 获取评论趋势
     * @param days 时间范围，默认7天
     * @return 评论趋势数据
     * 包含每天的评论数量和占比
     */
    @GetMapping("/stats/comment-trend")
    public Result<Map<String, Object>> getCommentTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        Map<String, Object> trend = dashboardService.getCommentTrend(days);
        return Result.success(trend);
    }

    /**
     * 获取用户角色分布
     * @return 用户角色分布数据
     * 包含每个角色的用户数量和占比
     */
    @GetMapping("/stats/user-role-distribution")
    public Result<List<Map<String, Object>>> getUserRoleDistribution() {
        List<Map<String, Object>> distribution = dashboardService.getUserRoleDistribution();
        return Result.success(distribution);
    }
}
