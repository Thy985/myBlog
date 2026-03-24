package com.xingchen.backend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {
    private Long id;
    private Long userId;
    private String title;
    private String summary;
    private String content;
    private String contentHtml;
    private String thumbnail;
    private String status;
    private Integer isTop;
    private Integer readNum;
    private Integer likeNum;
    private Integer commentNum;
    private Integer collectNum;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    // 关联信息
    private UserVO author;
    private CategoryVO category;
    private List<TagVO> tags;

    // 当前用户互动状态
    private Boolean isLiked;
    private Boolean isCollected;

    // 上下篇文章
    private ArticleVO preArticle;
    private ArticleVO nextArticle;
}
