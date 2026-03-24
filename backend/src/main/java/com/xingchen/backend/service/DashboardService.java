package com.xingchen.backend.service;

import java.util.List;
import java.util.Map;

/**
 * 全局数据仪表板服务接口
 */
public interface DashboardService {

    /**
     * 获取核心统计数据
     */
    Map<String, Object> getOverviewStats();

    /**
     * 获取文章发布趋势
     */
    Map<String, Object> getArticleTrend(Integer days);

    /**
     * 获取用户增长趋势
     */
    Map<String, Object> getUserTrend(Integer days);

    /**
     * 获取访问量趋势
     */
    Map<String, Object> getVisitTrend(Integer days);

    /**
     * 获取分类分布统计
     */
    List<Map<String, Object>> getCategoryDistribution();

    /**
     * 获取标签分布统计
     */
    List<Map<String, Object>> getTagDistribution();

    /**
     * 获取热门文章排行
     */
    List<Map<String, Object>> getHotArticles(Integer limit);

    /**
     * 获取活跃用户排行
     */
    List<Map<String, Object>> getActiveUsers(Integer limit);

    /**
     * 获取待审核内容统计
     */
    Map<String, Integer> getPendingReviewStats();

    /**
     * 获取系统运行状态
     */
    Map<String, Object> getSystemStatus();

    /**
     * 获取评论趋势
     */
    Map<String, Object> getCommentTrend(Integer days);

    /**
     * 获取用户角色分布
     */
    List<Map<String, Object>> getUserRoleDistribution();
}
