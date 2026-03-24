package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 飞书应用配置表（WebSocket 长连接模式）
 */
@Data
@Table(value = "feishu_app_config")
public class FeishuAppConfig {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("app_id")
    private String appId;

    @Column("app_secret")
    private String appSecret;

    @Column("enabled")
    private Integer enabled;

    @Column("connection_status")
    private String connectionStatus;

    @Column("last_connected_at")
    private LocalDateTime lastConnectedAt;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
