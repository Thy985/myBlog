package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("t_notification")
public class Notification {
    
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("type")
    private String type;
    
    @Column("title")
    private String title;
    
    @Column("content")
    private String content;
    
    @Column("related_id")
    private Long relatedId;//关联的ID
    
    @Column("related_type")
    private String relatedType;//关联的类型
    
    @Column("sender_id")
    private Long senderId;// 发送人ID
    
    @Column("is_read")
    private Integer isRead;
    
    @Column("create_time")
    private LocalDateTime createTime;
}
