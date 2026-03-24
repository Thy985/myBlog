package com.xingchen.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RedisScanUtil {

    private static final int SCAN_BATCH_SIZE = 1000;

    public static long deleteByPattern(RedisTemplate<String, Object> redisTemplate, String pattern) {
        AtomicLong deletedCount = new AtomicLong(0);
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_BATCH_SIZE)
                .build();

        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> connection.scan(options))) {
            if (cursor != null) {
                Set<String> keysToDelete = new HashSet<>();
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    keysToDelete.add(key);

                    if (keysToDelete.size() >= SCAN_BATCH_SIZE) {
                        deletedCount.addAndGet(deleteKeys(redisTemplate, keysToDelete));
                        keysToDelete.clear();
                    }
                }

                if (!keysToDelete.isEmpty()) {
                    deletedCount.addAndGet(deleteKeys(redisTemplate, keysToDelete));
                }
            }
        } catch (Exception e) {
            log.error("SCAN 操作异常，pattern: {}", pattern, e);
            throw new RuntimeException("缓存清理失败: " + e.getMessage());
        }

        return deletedCount.get();
    }

    public static long countKeys(RedisTemplate<String, Object> redisTemplate, String pattern) {
        AtomicLong count = new AtomicLong(0);
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_BATCH_SIZE)
                .build();

        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> connection.scan(options))) {
            if (cursor != null) {
                while (cursor.hasNext()) {
                    count.incrementAndGet();
                }
            }
        } catch (Exception e) {
            log.error("SCAN 计数异常，pattern: {}", pattern, e);
            throw new RuntimeException("缓存统计失败: " + e.getMessage());
        }

        return count.get();
    }

    private static long deleteKeys(RedisTemplate<String, Object> redisTemplate, Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted != null ? deleted : 0;
    }
}
