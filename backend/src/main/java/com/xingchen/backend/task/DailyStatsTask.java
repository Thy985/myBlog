package com.xingchen.backend.task;

import com.xingchen.backend.entity.DailyStats;
import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.RedisAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStatsTask {

    private final DailyStatsMapper dailyStatsMapper;
    private final PageViewMapper pageViewMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final RedisAnalyticsService redisAnalyticsService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String LOCK_KEY_PREFIX = "lock:dailyStats:";
    private static final long LOCK_EXPIRE_MINUTES = 10;

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void generateDailyStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateKey = yesterday.toString();
        String lockKey = LOCK_KEY_PREFIX + dateKey;
        
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("获取分布式锁失败，{} 的统计数据可能正在生成", yesterday);
            return;
        }
        
        try {
            log.info("开始生成每日统计数据...");
            
            DailyStats existStats = dailyStatsMapper.selectByDate(yesterday);
            if (existStats != null) {
                log.info("{} 的统计数据已存在，跳过", yesterday);
                return;
            }
            
            Long pvCount = redisAnalyticsService.getPvCount(dateKey);
            Long uvCount = redisAnalyticsService.getUvCount(dateKey);
            Long ipCount = redisAnalyticsService.getIpCount(dateKey);
            
            if (pvCount == 0) {
                log.warn("Redis 中无 {} 的 PV 数据，从数据库查询", yesterday);
                LocalDateTime dayStart = yesterday.atStartOfDay();
                LocalDateTime dayEnd = yesterday.atTime(LocalTime.MAX);
                pvCount = pageViewMapper.selectPvCount(dayStart, dayEnd.plusSeconds(1));
                uvCount = pageViewMapper.selectUvCount(dayStart, dayEnd.plusSeconds(1));
                ipCount = pageViewMapper.selectIpCount(dayStart, dayEnd.plusSeconds(1));
            }
            
            int newUserCount = userMapper.countByDate(yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX).plusSeconds(1));
            int articleCount = articleMapper.countByDate(yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX).plusSeconds(1));
            int commentCount = commentMapper.countByDate(yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX).plusSeconds(1));
            
            DailyStats stats = new DailyStats();
            stats.setStatsDate(yesterday);
            stats.setPvCount(pvCount != null ? pvCount.intValue() : 0);
            stats.setUvCount(uvCount != null ? uvCount.intValue() : 0);
            stats.setIpCount(ipCount != null ? ipCount.intValue() : 0);
            stats.setNewUserCount(newUserCount);
            stats.setArticlePublishCount(articleCount);
            stats.setCommentCount(commentCount);
            stats.setLikeCount(0);
            stats.setCreateTime(LocalDateTime.now());
            stats.setUpdateTime(LocalDateTime.now());
            
            dailyStatsMapper.insert(stats);
            
            redisAnalyticsService.syncToDatabase(yesterday, pvCount, uvCount, ipCount);
            
            log.info("{} 的统计数据生成完成：PV={}, UV={}, IP={}", yesterday, pvCount, uvCount, ipCount);
            
        } finally {
            redisTemplate.delete(lockKey);
            log.debug("已释放分布式锁：{}", lockKey);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldData() {
        log.info("清理过期的 Redis 统计数据...");
        redisAnalyticsService.cleanupExpiredKeys();
        log.info("过期数据清理完成");
    }
}
