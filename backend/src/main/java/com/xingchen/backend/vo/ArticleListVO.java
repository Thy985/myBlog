package com.xingchen.backend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleListVO {
    private Long id;
    private String title;
    private String summary;
    private String thumbnail;
    private String titleImage;
    private String status;
    private Integer isTop;
    private Integer readNum;
    private Integer likeNum;
    private Integer commentNum;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    private String description;
    private Integer viewCount;

    // 关联信息
    private String authorName;
    private String authorAvatar;
    private String categoryName;
    private List<String> tagNames;
}
