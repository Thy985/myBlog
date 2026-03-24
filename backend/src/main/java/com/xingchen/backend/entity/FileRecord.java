package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_file")
public class FileRecord {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("file_name")
    private String fileName;

    @Column("original_name")
    private String originalName;

    @Column("file_path")
    private String filePath;

    @Column("file_url")
    private String fileUrl;

    @Column("file_type")
    private String fileType; // IMAGE, DOCUMENT, VIDEO, AUDIO, OTHER

    @Column("mime_type")
    private String mimeType;// MIME类型

    @Column("file_size")
    private Long fileSize;

    @Column("category_id")
    private Long categoryId;

    @Column("download_count")
    private Integer downloadCount;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("is_deleted")
    private Integer isDeleted;
}
