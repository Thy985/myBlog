package com.xingchen.backend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.config.MinioConfig;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.FileRecord;
import com.xingchen.backend.mapper.FileRecordMapper;
import com.xingchen.backend.service.FileService;
import com.xingchen.backend.vo.FileVO;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.type:minio}")
    private String storageType;

    @Value("${file.access.url:}")
    private String accessUrl;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg", "bmp");
    private static final List<String> ALLOWED_FILE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "json", "xml", "zip", "rar",
            "mp4", "mp3", "wav", "avi", "mov"
    );

    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "class",
            "php", "asp", "aspx", "jsp", "cgi", "pl", "py", "rb"
    );

    @Override
    public FileVO uploadFile(Long userId, MultipartFile file, Long categoryId) {
        validateFile(file, false);

        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);
        String uuid = UUID.randomUUID().toString();
        String newFileName = uuid + "." + extension;
        String objectName = "files/" + uuid + "/" + newFileName;

        String fileUrl = saveFile(file, objectName);

        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setFileName(newFileName);
        record.setOriginalName(fileName);
        record.setFilePath(objectName);
        record.setFileUrl(fileUrl);
        record.setFileSize(file.getSize());
        record.setMimeType(file.getContentType());
        record.setDownloadCount(0);
        record.setCategoryId(categoryId);
        record.setCreateTime(java.time.LocalDateTime.now());
        record.setIsDeleted(0);

        fileRecordMapper.insert(record);
        return convertToVO(record);
    }

    @Override
    public FileVO uploadImage(Long userId, MultipartFile file) {
        validateFile(file, true);

        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);
        String uuid = UUID.randomUUID().toString();
        String newFileName = uuid + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectName = "images/" + newFileName;

        String fileUrl = saveFile(file, objectName);

        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setFileName(newFileName);
        record.setOriginalName(fileName);
        record.setFilePath(objectName);
        record.setFileUrl(fileUrl);
        record.setFileSize(file.getSize());
        record.setMimeType(file.getContentType());
        record.setDownloadCount(0);
        record.setCreateTime(java.time.LocalDateTime.now());
        record.setIsDeleted(0);

        fileRecordMapper.insert(record);
        return convertToVO(record);
    }

    @Override
    public FileVO uploadAvatar(Long userId, MultipartFile file) {
        validateAvatarFile(file);

        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);
        String uuid = UUID.randomUUID().toString();
        String newFileName = uuid + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectName = "avatars/" + newFileName;

        String fileUrl = saveFile(file, objectName);

        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setFileName(newFileName);
        record.setOriginalName(fileName);
        record.setFilePath(objectName);
        record.setFileUrl(fileUrl);
        record.setFileSize(file.getSize());
        record.setMimeType(file.getContentType());
        record.setDownloadCount(0);
        record.setCreateTime(java.time.LocalDateTime.now());
        record.setIsDeleted(0);

        fileRecordMapper.insert(record);
        return convertToVO(record);
    }

    /**
     * 保存文件到存储（根据配置选择 MinIO 或本地存储）
     */
    private String saveFile(MultipartFile file, String objectName) {
        if ("minio".equalsIgnoreCase(storageType)) {
            return saveToMinio(file, objectName);
        } else {
            return saveToLocal(file, objectName);
        }
    }

    /**
     * 保存文件到 MinIO
     */
    private String saveToMinio(MultipartFile file, String objectName) {
        try {
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            byte[] bytes = file.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .stream(bais, bytes.length, -1)
                            .contentType(contentType)
                            .build()
            );

            // 生成访问 URL（预签名 URL，有效期 7 天）
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );

            log.info("文件上传到 MinIO 成功: bucket={}, object={}", minioConfig.getBucket(), objectName);
            return url;
        } catch (Exception e) {
            log.error("MinIO 上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_SAVE_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 保存文件到本地磁盘
     */
    private String saveToLocal(MultipartFile file, String objectName) {
        try {
            // 将 objectName 转换为路径
            // objectName 格式: files/uuid/filename.ext 或 images/filename.ext
            String relativePath = objectName.replace("/", File.separator);

            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path targetPath = uploadDir.resolve(relativePath);

            // 安全检查：确保目标路径在上传目录内
            if (!targetPath.startsWith(uploadDir)) {
                throw new SecurityException("非法的文件路径");
            }

            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());

            log.info("文件保存到本地成功: {}", targetPath);
            // 返回相对路径作为 URL
            return "/" + objectName;
        } catch (IOException e) {
            log.error("本地文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_SAVE_ERROR);
        }
    }

    /**
     * 验证头像文件 - 不允许 SVG（防止 XSS 攻击）
     */
    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(fileName);
        if (extension == null || extension.isEmpty()) {
            throw new IllegalArgumentException("无法识别文件类型");
        }
        extension = extension.toLowerCase();

        // 头像不允许 SVG - SVG 可能包含恶意脚本
        if ("svg".equals(extension)) {
            throw new IllegalArgumentException("头像不支持 SVG 格式，请使用 JPG、PNG 或 WebP 格式");
        }

        // 使用图片验证（但不包含 SVG）
        if (!isValidAvatarImage(file)) {
            throw new IllegalArgumentException("文件内容与扩展名不匹配，可能不是有效的图片文件");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 10MB");
        }
    }

    /**
     * 验证头像图片 - 不包含 SVG
     */
    private boolean isValidAvatarImage(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[32];
            int read = is.read(header);
            if (read < 4) return false;

            String format = detectImageFormatByMagicNumber(header, read);
            if (format == null) return false;

            return !"svg".equals(format);
        } catch (IOException e) {
            log.error("验证头像文件失败", e);
            return false;
        }
    }

    private void validateFile(MultipartFile file, boolean imageOnly) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(fileName);
        if (extension == null || extension.isEmpty()) {
            throw new IllegalArgumentException("无法识别文件类型");
        }
        extension = extension.toLowerCase();

        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不允许上传该类型的文件: " + extension);
        }

        if (imageOnly) {
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("只能上传图片文件，支持的格式: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("图片大小不能超过 10MB");
            }
            if (!isValidImageFile(file)) {
                throw new IllegalArgumentException("文件内容与扩展名不匹配，可能不是有效的图片文件");
            }
        } else {
            if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("不支持该文件类型，支持的格式: " + String.join(", ", ALLOWED_FILE_EXTENSIONS));
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("文件大小不能超过 50MB");
            }
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return null;
    }

    private String detectImageFormatByMagicNumber(byte[] header, int read) {
        if (read < 4) return null;

        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return "jpg";
        }

        if (read >= 8 &&
                header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47 &&
                header[4] == (byte) 0x0D && header[5] == (byte) 0x0A &&
                header[6] == (byte) 0x1A && header[7] == (byte) 0x0A) {
            return "png";
        }

        if (read >= 6 &&
                header[0] == (byte) 0x47 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x38 &&
                (header[4] == (byte) 0x37 || header[4] == (byte) 0x39) &&
                header[5] == (byte) 0x61) {
            return "gif";
        }

        if (header[0] == (byte) 0x42 && header[1] == (byte) 0x4D) {
            return "bmp";
        }

        if (read >= 12 &&
                header[0] == (byte) 0x52 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x46 &&
                header[8] == (byte) 0x57 && header[9] == (byte) 0x45 &&
                header[10] == (byte) 0x42 && header[11] == (byte) 0x50) {
            return "webp";
        }

        if (header[0] == 0x3C) {
            String start = new String(header, 0, Math.min(read, 32), java.nio.charset.StandardCharsets.UTF_8).trim();
            if (start.startsWith("<svg") || start.startsWith("<?xml")) {
                return "svg";
            }
        }

        return null;
    }

    private boolean isValidMagicNumberForFormat(String format, byte[] header, int read) {
        switch (format.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return header[0] == (byte) 0xFF && header[1] == (byte) 0xD8;
            case "png":
                return read >= 8 &&
                        header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                        header[2] == (byte) 0x4E && header[3] == (byte) 0x47 &&
                        header[4] == (byte) 0x0D && header[5] == (byte) 0x0A &&
                        header[6] == (byte) 0x1A && header[7] == (byte) 0x0A;
            case "gif":
                return header[0] == (byte) 0x47 && header[1] == (byte) 0x49 && header[2] == (byte) 0x46;
            case "bmp":
                return header[0] == (byte) 0x42 && header[1] == (byte) 0x4D;
            case "webp":
                return read >= 12 &&
                        header[0] == (byte) 0x52 && header[1] == (byte) 0x49 &&
                        header[2] == (byte) 0x46 && header[3] == (byte) 0x46 &&
                        header[8] == (byte) 0x57 && header[9] == (byte) 0x45 &&
                        header[10] == (byte) 0x42 && header[11] == (byte) 0x50;
            case "svg":
                if (header[0] != 0x3C) return false;
                String content = new String(header, 0, Math.min(read, 32), java.nio.charset.StandardCharsets.UTF_8);
                return content.trim().startsWith("<svg") || content.trim().startsWith("<?xml");
            default:
                return false;
        }
    }

    private boolean isValidImageFile(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[32];
            int read = is.read(header);
            if (read < 4) return false;

            String fileName = file.getOriginalFilename();
            String extension = getFileExtension(fileName);

            if (extension != null) {
                extension = extension.toLowerCase();
                return isValidMagicNumberForFormat(extension, header, read);
            }

            return detectImageFormatByMagicNumber(header, read) != null;
        } catch (IOException e) {
            log.warn("无法读取文件头进行验证: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteFile(Long userId, Long id) {
        FileRecord record = fileRecordMapper.selectOneById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "无权限删除该文件");
        }

        // 删除 MinIO 中的文件
        if ("minio".equalsIgnoreCase(storageType)) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .object(record.getFilePath())
                                .build()
                );
                log.info("从 MinIO 删除文件成功: {}", record.getFilePath());
            } catch (Exception e) {
                log.warn("从 MinIO 删除文件失败: {}", e.getMessage());
            }
        }

        fileRecordMapper.deleteById(id);
    }

    @Override
    public List<FileVO> getFileList(Long userId, Long categoryId, String fileType, Integer page, Integer size) {
        int pageNum = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 10;
        int offset = (pageNum - 1) * pageSize;

        QueryWrapper queryWrapper = QueryWrapper.create()
                .from("t_file")
                .where("user_id = #{userId}")
                .and("is_deleted = 0");
        if (categoryId != null) {
            queryWrapper.and("category_id = #{categoryId}");
        }
        if (fileType != null && !fileType.isEmpty()) {
            queryWrapper.and("file_type = #{fileType}");
        }
        queryWrapper.orderBy("create_time", true)
                .limit(offset, pageSize);

        List<FileRecord> records = fileRecordMapper.selectListByQuery(queryWrapper);
        List<FileVO> result = new ArrayList<>();
        for (FileRecord record : records) {
            result.add(convertToVO(record));
        }
        return result;
    }

    @Override
    public List<FileVO> getImageList(Long userId, Integer page, Integer size) {
        int pageNum = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 10;
        int offset = (pageNum - 1) * pageSize;

        QueryWrapper queryWrapper = QueryWrapper.create()
                .from("t_file")
                .where("user_id = #{userId}")
                .and("is_deleted = 0")
                .and("file_type LIKE 'image/%'")
                .orderBy("create_time", true)
                .limit(offset, pageSize);

        List<FileRecord> records = fileRecordMapper.selectListByQuery(queryWrapper);
        List<FileVO> result = new ArrayList<>();
        for (FileRecord record : records) {
            result.add(convertToVO(record));
        }
        return result;
    }

    @Override
    public String getFileUrl(Long id) {
        FileRecord record = fileRecordMapper.selectOneById(id);
        if (record == null) {
            return null;
        }

        // 如果是 MinIO 存储且 URL 已过期，生成新的预签名 URL
        if ("minio".equalsIgnoreCase(storageType) && record.getFileUrl() != null && record.getFileUrl().contains("X-Amz-Signature")) {
            try {
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(minioConfig.getBucket())
                                .object(record.getFilePath())
                                .expiry(7, TimeUnit.DAYS)
                                .build()
                );
            } catch (Exception e) {
                log.warn("生成预签名 URL 失败: {}", e.getMessage());
                return record.getFileUrl();
            }
        }

        return record.getFileUrl();
    }

    @Override
    public void incrementDownloadCount(Long id) {
        FileRecord record = fileRecordMapper.selectOneById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        record.setDownloadCount(record.getDownloadCount() + 1);
        fileRecordMapper.update(record);
    }

    private FileVO convertToVO(FileRecord record) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(record, vo);
        // 如果是 MinIO URL，保持原 URL（预签名 URL 有效期 7 天）
        if ("minio".equalsIgnoreCase(storageType)) {
            vo.setFileUrl(record.getFileUrl());
        } else {
            vo.setFileUrl(record.getFilePath());
        }
        return vo;
    }
}
