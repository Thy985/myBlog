package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_page_view")
public class PageView {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("visitor_id")
    private String visitorId;

    @Column("user_id")
    private Long userId;

    @Column("page_url")
    private String pageUrl;

    @Column("referer")
    private String referer;// 引用页URL

    @Column("ip")
    private String ip;

    @Column("user_agent")
    private String userAgent;

    @Column("device")
    private String device;

    @Column("browser")
    private String browser;

    @Column("os")
    private String os;

    @Column("country")
    private String country;

    @Column("city")
    private String city;

    @Column("create_time")
    private LocalDateTime createTime;
}
