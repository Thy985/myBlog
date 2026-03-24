package com.xingchen.backend.service.impl;

import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalArticles", articleMapper.countAll());
        stats.put("totalUsers", userMapper.countAll());
        stats.put("totalCategories", categoryMapper.countAll());
        stats.put("totalTags", tagMapper.countAll());
        stats.put("totalComments", commentMapper.countAll());
        
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.put("todayArticles", articleMapper.countByDate(todayStart, LocalDateTime.now()));
        stats.put("todayUsers", userMapper.countByDate(todayStart, LocalDateTime.now()));
        stats.put("todayComments", commentMapper.countByDate(todayStart, LocalDateTime.now()));
        
        return stats;
    }

    @Override
    public Map<String, Object> getArticleTrend(Integer days) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            
            dates.add(date.format(formatter));
            counts.add(articleMapper.countByDate(start, end));
        }
        
        result.put("dates", dates);
        result.put("counts", counts);
        
        return result;
    }

    @Override
    public Map<String, Object> getUserTrend(Integer days) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            
            dates.add(date.format(formatter));
            counts.add(userMapper.countByDate(start, end));
        }
        
        result.put("dates", dates);
        result.put("counts", counts);
        
        return result;
    }

    @Override
    public Map<String, Object> getVisitTrend(Integer days) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(formatter));
            counts.add((int) (Math.random() * 1000));
        }
        
        result.put("dates", dates);
        result.put("counts", counts);
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getCategoryDistribution() {
        return categoryMapper.selectArticleCountGroupByCategory();
    }

    @Override
    public List<Map<String, Object>> getTagDistribution() {
        return tagMapper.selectArticleCountGroupByTag();
    }

    @Override
    public List<Map<String, Object>> getHotArticles(Integer limit) {
        return articleMapper.selectHotArticlesWithAuthor(limit);
    }

    @Override
    public List<Map<String, Object>> getActiveUsers(Integer limit) {
        return userMapper.selectActiveUsersWithArticleCount(limit);
    }

    @Override
    public Map<String, Integer> getPendingReviewStats() {
        Map<String, Integer> stats = new HashMap<>();
        
        stats.put("pendingArticles", articleMapper.countByStatus(0));
        stats.put("pendingComments", commentMapper.countByStatus(0));
        
        return stats;
    }

    @Override
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        
        status.put("jvmMaxMemory", maxMemory + "MB");
        status.put("jvmTotalMemory", totalMemory + "MB");
        status.put("jvmUsedMemory", usedMemory + "MB");
        status.put("jvmFreeMemory", freeMemory + "MB");
        status.put("jvmMemoryUsage", String.format("%.2f%%", (double) usedMemory / totalMemory * 100));
        
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        status.put("osName", osBean.getName());
        status.put("osVersion", osBean.getVersion());
        status.put("osArch", osBean.getArch());
        status.put("availableProcessors", osBean.getAvailableProcessors());
        
        double systemLoadAverage = osBean.getSystemLoadAverage();
        status.put("systemLoadAverage", systemLoadAverage >= 0 ? String.format("%.2f", systemLoadAverage) : "N/A");
        
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptime / (1000 * 60 * 60);
        long minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60);
        status.put("uptime", hours + "小时" + minutes + "分钟");
        
        return status;
    }

    @Override
    public Map<String, Object> getCommentTrend(Integer days) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            
            dates.add(date.format(formatter));
            counts.add(commentMapper.countByDate(start, end));
        }
        
        result.put("dates", dates);
        result.put("counts", counts);
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserRoleDistribution() {
        return userMapper.selectUserRoleDistribution();
    }
}
