package com.xingchen.backend.config;

import com.xingchen.backend.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * 记忆归档定时任务配置
 * <p>
 * 自动归档超过保留期的用户记忆数据
 */
@Configuration
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class MemoryArchiveConfig {

    private final MemoryService memoryService;

    // 默认保留 90 天
    private static final int DEFAULT_RETENTION_DAYS = 90;

    /**
     * 每天凌晨 2 点执行记忆归档任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveOldMemories() {
        log.info("开始执行记忆归档任务...");

        try {
            // 扫描所有用户目录
            Path workspace = Path.of("./data/agent");
            if (!Files.exists(workspace)) {
                log.info("工作目录不存在，跳过归档");
                return;
            }

            try (Stream<Path> paths = Files.list(workspace)) {
                paths.filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().startsWith("user_"))
                        .forEach(this::archiveUserMemory);
            }

            log.info("记忆归档任务完成");
        } catch (Exception e) {
            log.error("记忆归档任务失败", e);
        }
    }

    /**
     * 归档单个用户的记忆
     */
    private void archiveUserMemory(Path userDir) {
        try {
            String dirName = userDir.getFileName().toString();
            Long userId = Long.parseLong(dirName.replace("user_", ""));

            log.debug("归档用户 {} 的旧记忆", userId);
            memoryService.archiveOldMemories(userId, DEFAULT_RETENTION_DAYS);

        } catch (NumberFormatException e) {
            log.warn("无法解析用户ID: {}", userDir);
        } catch (Exception e) {
            log.error("归档用户记忆失败: {}", userDir, e);
        }
    }

    /**
     * 每周日凌晨 3 点清理空归档文件
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void cleanupEmptyArchives() {
        log.info("开始清理空归档文件...");

        try {
            Path workspace = Path.of("./data/agent");
            if (!Files.exists(workspace)) return;

            try (Stream<Path> userDirs = Files.list(workspace)) {
                userDirs.filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().startsWith("user_"))
                        .forEach(this::cleanupUserArchives);
            }

            log.info("空归档文件清理完成");
        } catch (Exception e) {
            log.error("清理空归档文件失败", e);
        }
    }

    /**
     * 清理用户的空归档文件
     */
    private void cleanupUserArchives(Path userDir) {
        Path archiveDir = userDir.resolve("archives");
        if (!Files.exists(archiveDir)) return;

        try (Stream<Path> files = Files.list(archiveDir)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isEmptyOrCorrupted)
                    .forEach(this::deleteFile);
        } catch (IOException e) {
            log.error("清理用户归档文件失败: {}", userDir, e);
        }
    }

    /**
     * 检查文件是否为空或损坏
     */
    private boolean isEmptyOrCorrupted(Path file) {
        try {
            long size = Files.size(file);
            if (size == 0) return true;

            // 检查是否为有效的 JSON 文件
            String content = Files.readString(file);
            return content.trim().isEmpty() ||
                    (!content.trim().startsWith("[") && !content.trim().startsWith("{"));
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * 安全删除文件
     */
    private void deleteFile(Path file) {
        try {
            Files.delete(file);
            log.info("删除空/损坏的归档文件: {}", file);
        } catch (IOException e) {
            log.error("删除文件失败: {}", file, e);
        }
    }
}
