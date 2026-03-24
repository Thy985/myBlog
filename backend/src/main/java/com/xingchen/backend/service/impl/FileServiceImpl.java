package com.xingchen.backend.service.impl;

import com.xingchen.backend.common.BusinessException;
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
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 2) return false;
            
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8) return true;
            if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && 
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47) return true;
            if (header[0] == (byte) 0x47 && header[1] == (byte) 0x49 && 
                header[2] == (byte) 0x46) return true;
            if (header[0] == (byte) 0x42 && header[1] == (byte) 0x4D) return true;
            if (header[0] == (byte) 0x52 && header[1] == (byte) 0x49 && 
                header[2] == (byte) 0x46 && header[3] == (byte) 0x46) return true;
            
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
