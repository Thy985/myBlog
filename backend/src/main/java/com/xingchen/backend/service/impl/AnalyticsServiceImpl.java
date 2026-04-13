package com.xingchen.backend.service.impl;

import com.xingchen.backend.component.PageViewBatchWriter;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.DailyStats;
import com.xingchen.backend.entity.PageView;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.DailyStatsMapper;
import com.xingchen.backend.mapper.PageViewMapper;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.AnalyticsService;
import com.xingchen.backend.service.RedisAnalyticsService;
import com.xingchen.backend.util.IpUtils;
import com.xingchen.backend.vo.AnalyticsVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final PageViewMapper pageViewMapper;
    private final DailyStatsMapper dailyStatsMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final RedisAnalyticsService redisAnalyticsService;
    private final PageViewBatchWriter batchWriter;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEDUP_WINDOW_MINUTES = 1;

    @Override
    public void recordPageView(String pageUrl, Long userId, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        String dateKey = LocalDate.now().format(DATE_FORMATTER);
        
        String visitorKey = (userId != null) ? "user:" + userId : "ip:" + ip;
        
        boolean isDuplicate = redisAnalyticsService.isDuplicateVisit(visitorKey, pageUrl, DEDUP_WINDOW_MINUTES);
        if (isDuplicate) {
            log.debug("检测到重复访问，已忽略 - 访客：{}, 页面：{}", visitorKey, pageUrl);
            return;
        }
        
        redisAnalyticsService.incrementPv(dateKey);
        
        if (userId != null) {
            redisAnalyticsService.addUv(dateKey, "user:" + userId);
        } else {
            redisAnalyticsService.addUv(dateKey, "visitor:" + ip);
        }
        redisAnalyticsService.addIp(dateKey, ip);
        
        PageView pageView = new PageView();
        pageView.setPageUrl(pageUrl);
        pageView.setUserId(userId);
        pageView.setIp(ip);
        pageView.setUserAgent(request.getHeader("User-Agent"));
        pageView.setReferer(request.getHeader("Referer"));
        pageView.setCreateTime(LocalDateTime.now());
        
        batchWriter.addPageView(pageView);
        
        log.debug("记录页面访问 - PV: {}, UV: {}, IP: {}", dateKey, 
            redisAnalyticsService.getPvCount(dateKey),
            redisAnalyticsService.getUvCount(dateKey));
    }

    /**
     * 获取网站分析概览数据
     * 
     * @return AnalyticsVO 包含总 PV、总 UV、总 IP、今日 PV、今日 UV、今日新增用户数的统计对象
     */
    @Override
    public AnalyticsVO getOverview() {
        AnalyticsVO vo = new AnalyticsVO();
        
        LocalDate today = LocalDate.now();
        String todayKey = today.format(DATE_FORMATTER);
        
        Long todayPv = redisAnalyticsService.getPvCount(todayKey);
        Long todayUv = redisAnalyticsService.getUvCount(todayKey);
        Long todayIp = redisAnalyticsService.getIpCount(todayKey);
        
        vo.setTodayPv(todayPv);
        vo.setTodayUv(todayUv);
        vo.setTodayIp(todayIp);
        
        Long totalPv = pageViewMapper.countAll();
        vo.setTotalPv(totalPv != null ? totalPv : 0L);
        
        Long totalUv = pageViewMapper.countDistinctUsers();
        vo.setTotalUv(totalUv != null ? totalUv : 0L);
        
        Long totalIp = pageViewMapper.countDistinctIps();
        vo.setTotalIp(totalIp != null ? totalIp : 0L);
        
        Long newUsersToday = userMapper.countNewUsersToday(today);
        vo.setNewUsersToday(newUsersToday != null ? newUsersToday : 0L);
        
        log.debug("获取概览数据 - 今日 PV: {}, UV: {}, IP: {}", todayPv, todayUv, todayIp);
        
        return vo;
    }

    /**
     * 获取指定日期范围内的趋势数据
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含每日PV、UV、新增用户数的趋势数据列表，每个元素为Map格式
     *         Map包含:date(日期字符串)、pv(访问量)、uv(独立访客数)、newUsers(新增用户数)
     */
    @Override
    public List<Map<String, Object>> getTrendData(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 查询指定日期范围内的每日统计数据
        List<DailyStats> stats = dailyStatsMapper.selectByDateRange(startDate, endDate);
        
        // 构建日期到统计数据的映射，便于快速查找
        Map<LocalDate, DailyStats> statsMap = new HashMap<>();
        for (DailyStats stat : stats) {
            statsMap.put(stat.getStatsDate(), stat);
        }
        
        // 遍历日期范围，填充每日数据
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", current.toString());
            
            DailyStats stat = statsMap.get(current);
            if (stat != null) {
                item.put("pv", stat.getPvCount() != null ? stat.getPvCount() : 0);
                item.put("uv", stat.getUvCount() != null ? stat.getUvCount() : 0);
                item.put("newUsers", stat.getNewUserCount() != null ? stat.getNewUserCount() : 0);
            } else {
                // 该日期无统计数据时，填充默认值0
                item.put("pv", 0);
                item.put("uv", 0);
                item.put("newUsers", 0);
            }
            
            result.add(item);
            current = current.plusDays(1);
        }
        
        return result;
    }

    /**
     * 获取设备类型统计信息
     * 
     * @return 包含设备类型统计的列表，每个元素包含设备类型、访问次数和占比
     */
    @Override
    public List<Map<String, Object>> getDeviceStats() {
        // 初始化结果列表并获取各设备类型的访问次数统计
        List<Map<String, Object>> result = new ArrayList<>();
        
        Map<String, Long> deviceCounts = pageViewMapper.countByDeviceType();
        
        // 计算总访问次数
        long total = deviceCounts.values().stream().mapToLong(Long::longValue).sum();
        
        // 构造每个设备类型的统计信息
        for (Map.Entry<String, Long> entry : deviceCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("device", entry.getKey());
            item.put("count", entry.getValue());
            item.put("percentage", total > 0 ? Math.round(entry.getValue() * 100.0 / total) : 0);
            result.add(item);
        }
        
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getSourceStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        Map<String, Long> sourceCounts = pageViewMapper.countByReferer();
        
        long total = sourceCounts.values().stream().mapToLong(Long::longValue).sum();
        
        for (Map.Entry<String, Long> entry : sourceCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            String source = entry.getKey();
            if (source == null || source.isEmpty()) {
                source = "直接访问";
            } else if (source.contains("google")) {
                source = "Google";
            } else if (source.contains("baidu")) {
                source = "百度";
            } else if (source.contains("bing")) {
                source = "Bing";
            } else {
                source = "其他";
            }
            item.put("source", source);
            item.put("count", entry.getValue());
            item.put("percentage", total > 0 ? Math.round(entry.getValue() * 100.0 / total) : 0);
            result.add(item);
        }
        
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        
        if (result.size() > 10) {
            result = result.subList(0, 10);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getHotArticles(Integer limit) {
        List<Article> articles = articleMapper.selectHotArticles(limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Article article : articles) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", article.getId());
            item.put("title", article.getTitle());
            item.put("readNum", article.getReadNum());
            result.add(item);
        }
        return result;
    }

    /**
     * 获取实时统计数据
     * 
     * @return 包含实时统计信息的Map，包含最近5分钟的页面访问量和活跃用户数
     */
    @Override
    public Map<String, Object> getRealtimeStats() {
        // 初始化统计结果Map
        Map<String, Object> stats = new HashMap<>();
        
        // 计算5分钟前的时间点并获取相关统计数据
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        Long recentPv = pageViewMapper.countSince(fiveMinutesAgo);
        stats.put("recentPv", recentPv != null ? recentPv : 0L);
        
        Long activeUsers = pageViewMapper.countActiveUsersSince(fiveMinutesAgo);
        stats.put("activeUsers", activeUsers != null ? activeUsers : 0L);
        
        return stats;
    }

    @Override
    public void recordPerformance(Map<String, Object> data) {
        log.debug("收到性能指标上报: {}", data);
    }

    @Override
    public void recordPerformanceErrors(Map<String, Object> data) {
        log.debug("收到性能错误上报: {}", data);
    }
}
