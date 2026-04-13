package com.xingchen.backend.service.impl;

import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.FileRecord;
import com.xingchen.backend.mapper.FileRecordMapper;
import com.xingchen.backend.service.FileService;
import com.xingchen.backend.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

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
        String relativePath = "/uploads/" + uuid + "/" + newFileName;
        
        String savedPath = saveFileToDisk(file, relativePath);
        
        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setFileName(newFileName);
        record.setOriginalName(fileName);
        record.setFilePath(savedPath);
        record.setFileUrl(savedPath);
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
        String relativePath = "/uploads/images/" + newFileName;
        
        String savedPath = saveFileToDisk(file, relativePath);
        
        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setFileName(newFileName);
        record.setOriginalName(fileName);
        record.setFilePath(savedPath);
        record.setFileUrl(savedPath);
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
        String relativePath = "/uploads/images/" + newFileName;

        String savedPath = saveFileToDisk(file, relativePath);

        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setFileName(newFileName);
        record.setOriginalName(fileName);
        record.setFilePath(savedPath);
        record.setFileUrl(savedPath);
        record.setFileSize(file.getSize());
        record.setMimeType(file.getContentType());
        record.setDownloadCount(0);
        record.setCreateTime(java.time.LocalDateTime.now());
        record.setIsDeleted(0);

        fileRecordMapper.insert(record);
        return convertToVO(record);
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

            // JPEG
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return true;
            }

            // PNG
            if (read >= 8 &&
                header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
                return true;
            }

            // GIF
            if (read >= 6 &&
                header[0] == (byte) 0x47 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 &&
                (header[4] == (byte) 0x37 || header[4] == (byte) 0x39) &&
                header[5] == (byte) 0x61) {
                return true;
            }

            // BMP
            if (header[0] == (byte) 0x42 && header[1] == (byte) 0x4D) {
                return true;
            }

            // WebP
            if (read >= 16 &&
                header[0] == (byte) 0x52 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x46 &&
                header[8] == (byte) 0x57 && header[9] == (byte) 0x45 &&
                header[10] == (byte) 0x42 && header[11] == (byte) 0x50) {
                return true;
            }

            return false;
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
    
    private boolean isValidImageFile(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            // 读取 32 字节用于魔数检测（比之前 8 字节更全面）
            byte[] header = new byte[32];
            int read = is.read(header);
            if (read < 4) return false;

            // ========== 魔数检测 ==========
            // JPEG: FFD8FF (with optional additional bytes before SOI)
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return true;
            }

            // PNG: 89 50 4E 47 0D 0A 1A 0A (8 bytes)
            if (read >= 8 &&
                header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47 &&
                header[4] == (byte) 0x0D && header[5] == (byte) 0x0A &&
                header[6] == (byte) 0x1A && header[7] == (byte) 0x0A) {
                return true;
            }

            // GIF87a: 47 49 46 38 37 61 (6 bytes)
            if (read >= 6 &&
                header[0] == (byte) 0x47 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x38 &&
                (header[4] == (byte) 0x37 || header[4] == (byte) 0x39) &&
                header[5] == (byte) 0x61) {
                return true;
            }

            // BMP: 42 4D (BM)
            if (header[0] == (byte) 0x42 && header[1] == (byte) 0x4D) {
                return true;
            }

            // WebP: 52 49 46 46 ... 57 45 42 50 (RIFF....WEBP)
            // 需要读取到 32 字节来确认完整的 WebP 签名
            if (read >= 16 &&
                header[0] == (byte) 0x52 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x46 &&
                header[8] == (byte) 0x57 && header[9] == (byte) 0x45 &&
                header[10] == (byte) 0x42 && header[11] == (byte) 0x50) {
                return true;
            }

            // SVG: 开头是 <svg 或 <?xml (文本格式，需读取部分内容判断)
            if (read >= 4 &&
                header[0] == 0x3C) { // '<'
                String start = new String(header, 0, Math.min(read, 32), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (start.startsWith("<svg") || start.startsWith("<?xml")) {
                    return true;
                }
            }

            // ========== 额外安全检测：验证文件扩展名与内容一致性 ==========
            String fileName = file.getOriginalFilename();
            String extension = getFileExtension(fileName);
            if (extension != null) {
                extension = extension.toLowerCase();
                // 如果魔数检测失败，但文件声称是某类型，进行一致性检查
                if ("jpg".equals(extension) || "jpeg".equals(extension)) {
                    // JPEG 文件必须以 FFD8 开头
                    if (header[0] != (byte) 0xFF || header[1] != (byte) 0xD8) return false;
                } else if ("png".equals(extension)) {
                    // PNG 必须符合完整 8 字节签名
                    if (read < 8 ||
                        !(header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                          header[2] == (byte) 0x4E && header[3] == (byte) 0x47)) return false;
                } else if ("gif".equals(extension)) {
                    if (header[0] != (byte) 0x47 || header[1] != (byte) 0x49 || header[2] != (byte) 0x46) return false;
                } else if ("bmp".equals(extension)) {
                    if (header[0] != (byte) 0x42 || header[1] != (byte) 0x4D) return false;
                } else if ("webp".equals(extension)) {
                    if (read < 12 ||
                        !(header[0] == (byte) 0x52 && header[1] == (byte) 0x49 &&
                          header[2] == (byte) 0x46 && header[3] == (byte) 0x46) ||
                        !(header[8] == (byte) 0x57 && header[9] == (byte) 0x45 &&
                          header[10] == (byte) 0x42 && header[11] == (byte) 0x50)) return false;
                } else if ("svg".equals(extension)) {
                    // SVG 必须是有效的 XML 格式
                    if (header[0] != 0x3C) return false;
                    String content = new String(header, 0, Math.min(read, 32), java.nio.charset.StandardCharsets.UTF_8);
                    if (!content.trim().startsWith("<svg") && !content.trim().startsWith("<?xml")) return false;
                }
            }

            // 有魔数匹配才通过
            return false;
        } catch (IOException e) {
            log.warn("无法读取文件头进行验证: {}", e.getMessage());
            return false;
        }
    }
    
    private String saveFileToDisk(MultipartFile file, String relativePath) {
        try {
            String pathWithoutPrefix = relativePath;
            if (relativePath.startsWith("/uploads/")) {
                pathWithoutPrefix = relativePath.substring("/uploads/".length());
            } else if (relativePath.startsWith("uploads/")) {
                pathWithoutPrefix = relativePath.substring("uploads/".length());
            }
            
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path targetPath = uploadDir.resolve(pathWithoutPrefix);
            
            if (!targetPath.startsWith(uploadDir)) {
                throw new SecurityException("非法的文件路径");
            }
            
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
            
            log.info("文件保存成功: {}", targetPath);
            return "/uploads/" + pathWithoutPrefix;
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_SAVE_ERROR);
        }
    }

    @Override
    public void deleteFile(Long userId, Long id) {
        FileRecord record = fileRecordMapper.selectOneById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "无权限删除该文件");
        }
        fileRecordMapper.deleteById(id);
    }

    @Override
    public List<FileVO> getFileList(Long userId, Long categoryId, String fileType, Integer page, Integer size) {
        List<FileRecord> records = fileRecordMapper.selectAll();
        List<FileVO> result = new ArrayList<>();
        for (FileRecord record : records) {
            if (record.getUserId() != null && record.getUserId().equals(userId)) {
                result.add(convertToVO(record));
            }
        }
        return result;
    }

    @Override
    public List<FileVO> getImageList(Long userId, Integer page, Integer size) {
        List<FileRecord> records = fileRecordMapper.selectAll();
        List<FileVO> result = new ArrayList<>();
        for (FileRecord record : records) {
            if (record.getUserId() != null && record.getUserId().equals(userId)
                    && record.getFileType() != null && record.getFileType().startsWith("image/")) {
                result.add(convertToVO(record));
            }
        }
        return result;
    }

    @Override
    public String getFileUrl(Long id) {
        FileRecord record = fileRecordMapper.selectOneById(id);
        return record != null ? record.getFilePath() : null;
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
        vo.setFileUrl(record.getFilePath());
        return vo;
    }
}
