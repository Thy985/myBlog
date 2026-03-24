package com.xingchen.backend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileVO {
    private Long id;
    private Long userId;
    private String fileName;
    private String originalName;
    private String fileUrl;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String fileSizeFormatted;
    private Long categoryId;
    private String categoryName;
    private Integer downloadCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
