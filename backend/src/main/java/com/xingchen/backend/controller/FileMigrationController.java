package com.xingchen.backend.controller;

import com.xingchen.backend.common.Result;
import com.xingchen.backend.service.FileMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件迁移控制器 - 将本地文件迁移到 MinIO
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
public class FileMigrationController {

    private final FileMigrationService fileMigrationService;

    /**
     * 执行文件迁移
     */
    @PostMapping("/files-to-minio")
    public Result<FileMigrationService.MigrationResult> migrateFilesToMinio() {
        log.info("开始执行文件迁移到 MinIO...");

        try {
            FileMigrationService.MigrationResult result = fileMigrationService.migrateAllFiles();

            if (result.failed() == 0) {
                return Result.success(result, "文件迁移成功");
            } else {
                return Result.success(result, "文件迁移完成，部分失败");
            }
        } catch (Exception e) {
            log.error("文件迁移失败", e);
            return Result.error(500, "文件迁移失败: " + e.getMessage());
        }
    }
}
