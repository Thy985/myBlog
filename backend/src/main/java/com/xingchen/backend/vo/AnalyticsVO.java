package com.xingchen.backend.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AnalyticsVO {
    private Long totalPv;
    private Long totalUv;
    private Long totalIp;
    private Long totalArticles;
    private Long totalComments;
    private Long todayPv;
    private Long todayUv;
    private Long todayIp;
    private Long newUsersToday;
    private List<Map<String, Object>> trendData;
    private List<ArticleListVO> hotArticles;
    private List<Map<String, Object>> deviceStats;
    private List<Map<String, Object>> sourceStats;
}
