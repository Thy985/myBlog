package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.constant.CacheKey;
import com.xingchen.backend.util.RedisScanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final RedisTemplate<String, Object> redisTemplate;

    @SaCheckRole("ADMIN")
    @PostMapping("/clear-all")
    public Result<String> clearAllCache(@RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return Result.error(400, "请添加 confirm=true 参数确认操作");
        }

        long deletedCount = RedisScanUtil.deleteByPattern(redisTemplate, "*");
        log.warn("管理员清理所有缓存，共 {} 条", deletedCount);
        return Result.success("已清理所有缓存，共 " + deletedCount + " 条");
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/clear-article")
    public Result<String> clearArticleCache(@RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return Result.error(400, "请添加 confirm=true 参数确认操作");
        }

        long count = 0;
        count += RedisScanUtil.deleteByPattern(redisTemplate, CacheKey.ARTICLE_PREFIX + "*");
        count += RedisScanUtil.deleteByPattern(redisTemplate, CacheKey.USER_ARTICLES.replace("%s", "*"));
        count += RedisScanUtil.deleteByPattern(redisTemplate, CacheKey.ARTICLE_HOT.replace("%s", "*"));

        log.warn("管理员清理文章缓存，共 {} 条", count);
        return Result.success("已清理文章缓存，共 " + count + " 条");
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/clear-user")
    public Result<String> clearUserCache(@RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return Result.error(400, "请添加 confirm=true 参数确认操作");
        }

        long count = 0;
        count += RedisScanUtil.deleteByPattern(redisTemplate, CacheKey.USER_PREFIX + "*");
        count += RedisScanUtil.deleteByPattern(redisTemplate, CacheKey.USER_COLLECTS.replace("%s", "*"));

        log.warn("管理员清理用户缓存，共 {} 条", count);
        return Result.success("已清理用户缓存，共 " + count + " 条");
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/clear-analytics")
    public Result<String> clearAnalyticsCache(@RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return Result.error(400, "请添加 confirm=true 参数确认操作");
        }

        long count = 0;
        count += RedisScanUtil.deleteByPattern(redisTemplate, CacheKey.ANALYTICS_PREFIX + "*");

        log.warn("管理员清理统计缓存，共 {} 条", count);
        return Result.success("已清理统计缓存，共 " + count + " 条");
    }

    @SaCheckRole("ADMIN")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalKeys", RedisScanUtil.countKeys(redisTemplate, "*"));
        stats.put("articleKeys", RedisScanUtil.countKeys(redisTemplate, CacheKey.ARTICLE_PREFIX + "*"));
        stats.put("userKeys", RedisScanUtil.countKeys(redisTemplate, CacheKey.USER_PREFIX + "*"));
        stats.put("analyticsKeys", RedisScanUtil.countKeys(redisTemplate, CacheKey.ANALYTICS_PREFIX + "*"));
        stats.put("kbKeys", RedisScanUtil.countKeys(redisTemplate, CacheKey.KB_PREFIX + "*"));
        stats.put("aiKeys", RedisScanUtil.countKeys(redisTemplate, CacheKey.AI_PREFIX + "*"));
        stats.put("verificationKeys", RedisScanUtil.countKeys(redisTemplate, CacheKey.VERIFICATION_PREFIX + "*"));

        return Result.success(stats);
    }
}
