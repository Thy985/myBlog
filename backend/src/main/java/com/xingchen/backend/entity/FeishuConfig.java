package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "feishu_config")
public class FeishuConfig {
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("webhook_url")
    private String webhookUrl;
    
    @Column("enabled")
    private Integer enabled;
    
    @Column("create_time")
    private LocalDateTime createTime;
    
    @Column("update_time")
    private LocalDateTime updateTime;
}
