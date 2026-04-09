package com.xingchen.backend.listener;

import com.xingchen.backend.entity.Article;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmUpListener implements ApplicationListener<ApplicationReadyEvent> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("开始缓存预热...");
        
        try {
            warmUpHotArticles();
            warmUpArticleList();
            log.info("缓存预热完成");
        } catch (Exception e) {
            log.error("缓存预热失败：{} - {}", e.getClass().getName(), e.getMessage(), e);
            // 缓存预热失败不应导致应用退出，只记录错误
            log.warn("缓存预热失败，应用将继续启动");
        }
    }

    private void warmUpHotArticles() {
        long startTime = System.currentTimeMillis();
        
        try {
            List<ArticleVO> hotArticles = articleService.getHotArticles(10);
            
            if (hotArticles != null && !hotArticles.isEmpty()) {
                redisTemplate.opsForValue().set("hotArticles::10", hotArticles, 5 + randomOffset(2), TimeUnit.MINUTES);
                log.info("热门文章缓存预热完成，共 {} 条，耗时 {}ms", hotArticles.size(), System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            log.error("热门文章预热失败：{}", e.getMessage());
        }
    }

    private void warmUpArticleList() {
        long startTime = System.currentTimeMillis();
        
        try {
            List<Article> recentArticles = articleMapper.selectPublicArticlesPage(0, 20);
            
            if (recentArticles != null && !recentArticles.isEmpty()) {
                String cacheKey = "articleList::recent::1";
                redisTemplate.opsForValue().set(cacheKey, recentArticles, 10 + randomOffset(3), TimeUnit.MINUTES);
                log.info("最新文章列表缓存预热完成，共 {} 条，耗时 {}ms", recentArticles.size(), System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            log.error("文章列表预热失败：{}", e.getMessage());
        }
    }

    private long randomOffset(long maxMinutes) {
        return new java.util.Random().nextInt((int) maxMinutes);
    }
}
