package com.xingchen.backend.service;

import com.xingchen.backend.vo.AnalyticsVO;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AnalyticsService {
    void recordPageView(String pageUrl, Long userId, HttpServletRequest request);

    AnalyticsVO getOverview();

    List<Map<String, Object>> getTrendData(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getDeviceStats();

    List<Map<String, Object>> getSourceStats();

    List<Map<String, Object>> getHotArticles(Integer limit);

    Map<String, Object> getRealtimeStats();

    void recordPerformance(Map<String, Object> data);

    void recordPerformanceErrors(Map<String, Object> data);
}
