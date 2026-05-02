package com.xingchen.backend.service;

import com.xingchen.backend.config.MinioConfig;
import com.xingchen.backend.entity.BlogSetting;
import com.xingchen.backend.entity.FileRecord;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.mapper.BlogSettingMapper;
import com.xingchen.backend.mapper.FileRecordMapper;
import com.xingchen.backend.mapper.UserMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件迁移服务 - 将本地文件迁移到 MinIO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileMigrationService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileRecordMapper fileRecordMapper;
    private final UserMapper userMapper;
    private final BlogSettingMapper blogSettingMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 迁移结果
     */
    public record MigrationResult(
            int total,
            int success,
            int failed,
            List<String> errors
    ) {}

    /**
     * 迁移所有本地文件到 MinIO
     */
    public MigrationResult migrateAllFiles() {
        log.info("开始文件迁移到 MinIO...");
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;

        // 1. 迁移 FileRecord 表中的文件
        List<FileRecord> fileRecords = fileRecordMapper.selectAll();
        log.info("找到 {} 个文件记录需要迁移", fileRecords.size());

        for (FileRecord record : fileRecords) {
            try {
                if (migrateFileRecord(record)) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                String error = String.format("迁移文件记录失败 [id=%d]: %s", record.getId(), e.getMessage());
                errors.add(error);
                log.error(error, e);
            }
        }

        // 2. 迁移用户头像
        List<User> users = userMapper.selectAll();
        log.info("找到 {} 个用户需要检查头像", users.size());

        for (User user : users) {
            try {
                if (migrateUserAvatar(user)) {
                    success++;
                }
            } catch (Exception e) {
                failed++;
                String error = String.format("迁移用户头像失败 [id=%d]: %s", user.getId(), e.getMessage());
                errors.add(error);
                log.error(error, e);
            }
        }

        // 3. 迁移博客设置中的头像和 Logo
        try {
            BlogSetting setting = blogSettingMapper.selectOneById(1);
            if (setting != null) {
                if (migrateBlogSettingAvatar(setting)) {
                    success++;
                }
            }
        } catch (Exception e) {
            failed++;
            String error = String.format("迁移博客设置失败: %s", e.getMessage());
            errors.add(error);
            log.error(error, e);
        }

        int total = fileRecords.size() + users.size() + 1;
        log.info("文件迁移完成: 总计={}, 成功={}, 失败={}", total, success, failed);

        return new MigrationResult(total, success, failed, errors);
    }

    /**
     * 迁移单个文件记录
     */
    private boolean migrateFileRecord(FileRecord record) throws Exception {
        String fileUrl = record.getFileUrl();

        // 已经是 HTTP URL，跳过
        if (fileUrl != null && fileUrl.startsWith("http")) {
            log.debug("文件已经是 HTTP URL，跳过: {}", fileUrl);
            return true;
        }

        // 构建本地文件路径
        String filePath = record.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            log.warn("文件路径为空，跳过: id={}", record.getId());
            return false;
        }

        Path localPath = Paths.get(uploadPath, filePath);
        if (!Files.exists(localPath)) {
            log.warn("本地文件不存在: {}", localPath);
            return false;
        }

        // 生成 MinIO 对象名
        String objectName = filePath.startsWith("/") ? filePath.substring(1) : filePath;

        // 上传到 MinIO
        uploadToMinio(localPath, objectName, record.getMimeType());

        // 生成新的 URL
        String newUrl = buildMinioUrl(objectName);

        // 更新数据库
            record.setFileUrl(newUrl);
            fileRecordMapper.update(record);

        log.info("文件迁移成功: {} -> {}", localPath, newUrl);
        return true;
    }

    /**
     * 迁移用户头像
     */
    private boolean migrateUserAvatar(User user) throws Exception {
        String avatar = user.getAvatar();
        if (avatar == null || avatar.isEmpty() || avatar.startsWith("http")) {
            return true; // 无需迁移
        }

        // 提取文件路径
        String filePath;
        if (avatar.startsWith("/api/file/")) {
            filePath = avatar.substring("/api/file/".length());
        } else if (avatar.startsWith("/")) {
            filePath = avatar.substring(1);
        } else {
            filePath = avatar;
        }

        Path localPath = Paths.get(uploadPath, filePath);
        if (!Files.exists(localPath)) {
            log.warn("用户头像文件不存在: {}", localPath);
            return false;
        }

        // 上传到 MinIO
        String objectName = "avatars/" + Paths.get(filePath).getFileName();
        uploadToMinio(localPath, objectName, "image/png");

        // 更新用户头像 URL
            String newUrl = buildMinioUrl(objectName);
            user.setAvatar(newUrl);
            userMapper.update(user);

        log.info("用户头像迁移成功 [userId={}]: {} -> {}", user.getId(), avatar, newUrl);
        return true;
    }

    /**
     * 迁移博客设置中的头像和 Logo
     */
    private boolean migrateBlogSettingAvatar(BlogSetting setting) throws Exception {
        boolean migrated = false;

        // 迁移头像
        String avatar = setting.getAvatar();
        if (avatar != null && !avatar.isEmpty() && !avatar.startsWith("http")) {
            String newUrl = migrateSettingImage(avatar, "blog");
            if (newUrl != null) {
                setting.setAvatar(newUrl);
                migrated = true;
                log.info("博客头像迁移成功: {} -> {}", avatar, newUrl);
            }
        }

        // 迁移 Logo
        String logo = setting.getLogo();
        if (logo != null && !logo.isEmpty() && !logo.startsWith("http")) {
            String newUrl = migrateSettingImage(logo, "blog");
            if (newUrl != null) {
                setting.setLogo(newUrl);
                migrated = true;
                log.info("博客 Logo 迁移成功: {} -> {}", logo, newUrl);
            }
        }

        if (migrated) {
            blogSettingMapper.update(setting);
        }

        return migrated;
    }

    /**
     * 迁移设置中的图片
     */
    private String migrateSettingImage(String imageUrl, String folder) throws Exception {
        String filePath;
        if (imageUrl.startsWith("/api/file/")) {
            filePath = imageUrl.substring("/api/file/".length());
        } else if (imageUrl.startsWith("/")) {
            filePath = imageUrl.substring(1);
        } else {
            filePath = imageUrl;
        }

        Path localPath = Paths.get(uploadPath, filePath);
        if (!Files.exists(localPath)) {
            log.warn("设置图片文件不存在: {}", localPath);
            return null;
        }

        String objectName = folder + "/" + Paths.get(filePath).getFileName();
        uploadToMinio(localPath, objectName, "image/jpeg");

        return buildMinioUrl(objectName);
    }

    /**
     * 上传文件到 MinIO
     */
    private void uploadToMinio(Path localPath, String objectName, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .stream(Files.newInputStream(localPath), Files.size(localPath), -1)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .build()
        );
    }

    /**
     * 构建 MinIO URL
     */
    private String buildMinioUrl(String objectName) {
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;
    }
}
