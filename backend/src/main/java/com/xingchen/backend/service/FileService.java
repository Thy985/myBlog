package com.xingchen.backend.service;

import com.xingchen.backend.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    FileVO uploadFile(Long userId, MultipartFile file, Long categoryId);

    FileVO uploadImage(Long userId, MultipartFile file);

    /**
     * 上传头像图片 - 不允许 SVG（防止 XSS 攻击）
     */
    FileVO uploadAvatar(Long userId, MultipartFile file);

    void deleteFile(Long userId, Long id);

    List<FileVO> getFileList(Long userId, Long categoryId, String fileType, Integer page, Integer size);

    List<FileVO> getImageList(Long userId, Integer page, Integer size);

    String getFileUrl(Long id);

    void incrementDownloadCount(Long id);
}
