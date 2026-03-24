package com.xingchen.backend.component;

import com.xingchen.backend.entity.PageView;
import com.xingchen.backend.mapper.PageViewMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PageViewBatchWriter {

    private final PageViewMapper pageViewMapper;
    
    private static final int BATCH_SIZE = 500;
    private static final int FLUSH_INTERVAL_SECONDS = 5;
    private static final int QUEUE_CAPACITY = 10000;
    
    private final BlockingQueue<PageView> buffer = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r, "PageViewBatchWriter");
        thread.setDaemon(true);
        return thread;
    });
    
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::batchWrite, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("PageView 批量写入组件已启动，批量大小：{}，刷新间隔：{}秒", BATCH_SIZE, FLUSH_INTERVAL_SECONDS);
    }

    public void addPageView(PageView pageView) {
        boolean success = buffer.offer(pageView);
        if (!success) {
            log.warn("PageView 缓冲区已满，直接写入数据库");
            try {
                pageViewMapper.insert(pageView);
            } catch (Exception e) {
                log.error("PageView 直接写入失败：{}", e.getMessage());
            }
        }
    }

    public void batchWrite() {
        if (!running) {
            return;
        }

        List<PageView> batch = new ArrayList<>(BATCH_SIZE);
        int drained = buffer.drainTo(batch, BATCH_SIZE);
        
        if (!batch.isEmpty()) {
            try {
                for (PageView pageView : batch) {
                    pageViewMapper.insert(pageView);
                }
                log.debug("批量写入 PageView: {} 条", batch.size());
            } catch (Exception e) {
                log.error("批量写入 PageView 失败：{}", e.getMessage());
                for (PageView pageView : batch) {
                    try {
                        pageViewMapper.insert(pageView);
                    } catch (Exception ex) {
                        log.error("单条写入 PageView 失败：{}", ex.getMessage());
                    }
                }
            }
        }
    }

    public void flushAndStop() {
        running = false;
        
        List<PageView> remaining = new ArrayList<>();
        buffer.drainTo(remaining);
        
        if (!remaining.isEmpty()) {
            log.info("正在写入剩余的 {} 条 PageView 记录", remaining.size());
            for (PageView pageView : remaining) {
                try {
                    pageViewMapper.insert(pageView);
                } catch (Exception e) {
                    log.error("写入剩余 PageView 失败：{}", e.getMessage());
                }
            }
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("PageView 批量写入组件已停止");
    }

    @PreDestroy
    public void destroy() {
        flushAndStop();
    }

    public int getQueueSize() {
        return buffer.size();
    }
}
