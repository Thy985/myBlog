package com.xingchen.backend.job;

import com.xingchen.backend.entity.Article;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.messaging.EmbeddingProducer;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 向量同步任务
 * 定期同步存量文章到向量数据库
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VectorSyncJob {

    private final ArticleMapper articleMapper;
    private final EmbeddingProducer embeddingProducer;

    // 同步批次大小
    private static final int BATCH_SIZE = 100;

    /**
     * 全量同步（每天凌晨 3 点执行）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "fullSync", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void fullSync() {
        log.info("开始全量向量同步任务");
        long startTime = System.currentTimeMillis();

        try {
            int offset = 0;
            int totalSynced = 0;

            while (true) {
                List<Article> articles = articleMapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("status", 1)  // 已发布
                                .orderBy("id")
                                .limit(BATCH_SIZE)
                                .offset(offset)
                );

                if (articles.isEmpty()) {
                    break;
                }

                for (Article article : articles) {
                    try {
                        embeddingProducer.sendIndexTask(
                                article.getId(),
                                article.getTitle(),
                                article.getDescription(),
                                "article"
                        );
                        totalSynced++;
                    } catch (Exception e) {
                        log.error("同步文章失败: articleId={}", article.getId(), e);
                    }
                }

                offset += BATCH_SIZE;
                log.info("已同步 {} 篇文章", totalSynced);

                // 避免过快发送
                Thread.sleep(1000);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("全量向量同步完成: total={}, duration={}ms", totalSynced, duration);

        } catch (Exception e) {
            log.error("全量向量同步失败", e);
        }
    }

    /**
     * 增量同步（每 10 分钟执行）
     */
    @Scheduled(cron = "0 */10 * * * ?")
    @SchedulerLock(name = "incrementalSync", lockAtLeastFor = "1m", lockAtMostFor = "9m")
    public void incrementalSync() {
        log.info("开始增量向量同步任务");

        try {
            // 获取最近 10 分钟更新的文章
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

            List<Article> articles = articleMapper.selectListByQuery(
                    QueryWrapper.create()
                            .ge("update_time", tenMinutesAgo)
                            .eq("status", 1)
            );

            for (Article article : articles) {
                try {
                    embeddingProducer.sendIndexTask(
                            article.getId(),
                            article.getTitle(),
                            article.getDescription(),
                            "article"
                    );
                    log.debug("增量同步文章: articleId={}", article.getId());
                } catch (Exception e) {
                    log.error("增量同步文章失败: articleId={}", article.getId(), e);
                }
            }

            log.info("增量向量同步完成: count={}", articles.size());

        } catch (Exception e) {
            log.error("增量向量同步失败", e);
        }
    }
}
