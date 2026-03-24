package com.xingchen.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisAnalyticsService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String PV_KEY_PREFIX = "analytics:pv:";
    private static final String UV_KEY_PREFIX = "analytics:uv:";
    private static final String IP_KEY_PREFIX = "analytics:ip:";
    private static final String DEDUP_KEY_PREFIX = "analytics:dedup:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void incrementPv(String dateKey) {
        String key = PV_KEY_PREFIX + dateKey;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    public void addUv(String dateKey, Object visitorId) {
        String key = UV_KEY_PREFIX + dateKey;
        redisTemplate.opsForHyperLogLog().add(key, visitorId);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    public void addIp(String dateKey, String ip) {
        String key = IP_KEY_PREFIX + dateKey;
        redisTemplate.opsForHyperLogLog().add(key, ip);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    public Long getPvCount(String dateKey) {
        String key = PV_KEY_PREFIX + dateKey;
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public Long getUvCount(String dateKey) {
        String key = UV_KEY_PREFIX + dateKey;
        return redisTemplate.opsForHyperLogLog().size(key);
    }

    public Long getIpCount(String dateKey) {
        String key = IP_KEY_PREFIX + dateKey;
        return redisTemplate.opsForHyperLogLog().size(key);
    }

    public boolean isDuplicateVisit(String visitorKey, String pageUrl, int windowMinutes) {
        String dateKey = LocalDate.now().format(DATE_FORMATTER);
        String minuteKey = String.valueOf(System.currentTimeMillis() / (windowMinutes * 60 * 1000));
        String dedupKey = DEDUP_KEY_PREFIX + dateKey + ":" + visitorKey + ":" + pageUrl + ":" + minuteKey;
        
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", windowMinutes, TimeUnit.MINUTES);
        return Boolean.FALSE.equals(isNew);
    }

    public void syncToDatabase(LocalDate date, Long pv, Long uv, Long ip) {
        String dateKey = date.format(DATE_FORMATTER);
        
        redisTemplate.opsForValue().set(PV_KEY_PREFIX + dateKey + ":sync", pv);
        redisTemplate.opsForValue().set(UV_KEY_PREFIX + dateKey + ":sync", uv);
        redisTemplate.opsForValue().set(IP_KEY_PREFIX + dateKey + ":sync", ip);
        
        redisTemplate.expire(PV_KEY_PREFIX + dateKey + ":sync", 30, TimeUnit.DAYS);
        redisTemplate.expire(UV_KEY_PREFIX + dateKey + ":sync", 30, TimeUnit.DAYS);
        redisTemplate.expire(IP_KEY_PREFIX + dateKey + ":sync", 30, TimeUnit.DAYS);
    }

    public void cleanupExpiredKeys() {
        LocalDate expiryDate = LocalDate.now().minusDays(30);
        String expiryDateKey = expiryDate.format(DATE_FORMATTER);
        
        String pvKey = PV_KEY_PREFIX + expiryDateKey;
        String uvKey = UV_KEY_PREFIX + expiryDateKey;
        String ipKey = IP_KEY_PREFIX + expiryDateKey;
        
        redisTemplate.delete(pvKey);
        redisTemplate.delete(uvKey);
        redisTemplate.delete(ipKey);
        
        log.info("已清理 {} 的 Redis 统计数据", expiryDateKey);
    }
}
