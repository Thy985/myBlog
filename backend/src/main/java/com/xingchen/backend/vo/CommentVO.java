package com.xingchen.backend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long articleId;
    private Long userId;
    private Long parentId;
    private Long rootId;
    private String content;
    private Integer likeNum;
    private Integer replyNum;
    private String status;
    private String device;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    private String articleTitle;

    private String username;
    private String nickname;
    private String avatar;

    private String replyToUsername;
    private String replyToNickname;

    private List<CommentVO> replies;

    private Boolean isLiked;
}
