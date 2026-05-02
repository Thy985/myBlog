package com.xingchen.backend.service;

import com.xingchen.backend.entity.BlogSetting;
import com.xingchen.backend.entity.FileRecord;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.mapper.BlogSettingMapper;
import com.xingchen.backend.mapper.FileRecordMapper;
import com.xingchen.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * URL 更新服务 - 将数据库中的本地文件 URL 更新为 MinIO URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlUpdateService {

    private final FileRecordMapper fileRecordMapper;
    private final UserMapper userMapper;
    private final BlogSettingMapper blogSettingMapper;

    @Value("${file.minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${file.minio.bucket:myblog}")
    private String bucketName;

    /**
     * URL 更新结果
     */
    public record UpdateResult(
            int blogSettingUpdated,
            int userAvatarUpdated,
            int fileRecordUpdated,
            List<String> errors
    ) {}

    /**
     * 更新所有 URL 为 MinIO URL
     */
    @Transactional
    public UpdateResult updateAllUrls() {
        log.info("开始更新 URL 为 MinIO URL...");
        List<String> errors = new ArrayList<>();
        int blogSettingUpdated = 0;
        int userAvatarUpdated = 0;
        int fileRecordUpdated = 0;

        // 1. 更新博客设置
        try {
            BlogSetting setting = blogSettingMapper.selectOneById(1);
            if (setting != null) {
                String avatar = setting.getAvatar();
                if (avatar != null && !avatar.startsWith("http")) {
                    String newUrl = convertToMinioUrl(avatar);
                    setting.setAvatar(newUrl);
                    blogSettingMapper.update(setting);
                    blogSettingUpdated++;
                    log.info("博客头像 URL 更新: {} -> {}", avatar, newUrl);
                }

                String logo = setting.getLogo();
                if (logo != null && !logo.startsWith("http")) {
                    String newUrl = convertToMinioUrl(logo);
                    setting.setLogo(newUrl);
                    blogSettingMapper.update(setting);
                    blogSettingUpdated++;
                    log.info("博客 Logo URL 更新: {} -> {}", logo, newUrl);
                }
            }
        } catch (Exception e) {
            errors.add("更新博客设置失败: " + e.getMessage());
            log.error("更新博客设置失败", e);
        }

        // 2. 更新用户头像
        try {
            List<User> users = userMapper.selectAll();
            for (User user : users) {
                String avatar = user.getAvatar();
                if (avatar != null && !avatar.isEmpty() && !avatar.startsWith("http")) {
                    String newUrl = convertToMinioUrl(avatar);
                    user.setAvatar(newUrl);
                    userMapper.update(user);
                    userAvatarUpdated++;
                    log.info("用户头像 URL 更新 [userId={}]: {} -> {}", user.getId(), avatar, newUrl);
                }
            }
        } catch (Exception e) {
            errors.add("更新用户头像失败: " + e.getMessage());
            log.error("更新用户头像失败", e);
        }

        // 3. 更新文件记录
        try {
            List<FileRecord> records = fileRecordMapper.selectAll();
            for (FileRecord record : records) {
                String fileUrl = record.getFileUrl();
                if (fileUrl != null && !fileUrl.startsWith("http")) {
                    String newUrl = convertToMinioUrl(fileUrl);
                    record.setFileUrl(newUrl);
                    fileRecordMapper.update(record);
                    fileRecordUpdated++;
                    log.info("文件记录 URL 更新 [id={}]: {} -> {}", record.getId(), fileUrl, newUrl);
                }
            }
        } catch (Exception e) {
            errors.add("更新文件记录失败: " + e.getMessage());
            log.error("更新文件记录失败", e);
        }

        log.info("URL 更新完成: 博客设置={}, 用户头像={}, 文件记录={}",
                blogSettingUpdated, userAvatarUpdated, fileRecordUpdated);

        return new UpdateResult(blogSettingUpdated, userAvatarUpdated, fileRecordUpdated, errors);
    }

    /**
     * 将本地路径转换为 MinIO URL
     */
    private String convertToMinioUrl(String localPath) {
        // 移除 /api/file/ 前缀
        String path = localPath;
        if (path.startsWith("/api/file/")) {
            path = path.substring("/api/file/".length());
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return minioEndpoint + "/" + bucketName + "/" + path;
    }
}
