package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.aspect.RateLimit;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.service.AnalyticsService;
import com.xingchen.backend.vo.AnalyticsVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
/**
 * 统计接口
 * 提供网站访问统计相关的接口
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    /**
     * 访问统计
     * @param pageUrl 页面URL
     * @param request HttpServletRequest对象
     * @return 访问成功
     */
    @PostMapping("/pv")
    @RateLimit(perMinute = 120, message = "请求过于频繁")
    public Result<Boolean> recordPageView(@RequestParam String pageUrl, HttpServletRequest request) {
        Long userId = null;
        if (StpUtil.isLogin()) {
            userId = StpUtil.getLoginIdAsLong();
        }
        analyticsService.recordPageView(pageUrl, userId, request);
        return Result.success(true);
    }

    @GetMapping("/overview")
    public Result<AnalyticsVO> getOverview() {
        return Result.success(analyticsService.getOverview());
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrendData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(analyticsService.getTrendData(startDate, endDate));
    }

    @GetMapping("/device")
    public Result<List<Map<String, Object>>> getDeviceStats() {
        return Result.success(analyticsService.getDeviceStats());
    }

    @GetMapping("/source")
    public Result<List<Map<String, Object>>> getSourceStats() {
        return Result.success(analyticsService.getSourceStats());
    }

    @GetMapping("/hot-articles")
    public Result<List<Map<String, Object>>> getHotArticles(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(analyticsService.getHotArticles(limit));
    }

    @GetMapping("/realtime")
    public Result<Map<String, Object>> getRealtimeStats() {
        return Result.success(analyticsService.getRealtimeStats());
    }
}
