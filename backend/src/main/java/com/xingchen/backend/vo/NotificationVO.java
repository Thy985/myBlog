package com.xingchen.backend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private Long userId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private String relatedType;
    private Boolean isRead;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
