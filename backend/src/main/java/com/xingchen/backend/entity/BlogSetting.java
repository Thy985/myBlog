package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_blog_setting")
public class BlogSetting {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("blog_name")
    private String blogName;

    @Column("author")
    private String author;

    @Column("introduction")
    private String introduction;

    @Column("avatar")
    private String avatar;

    @Column("logo")
    private String logo;

    @Column("favicon")
    private String favicon;

    @Column("github_home")
    private String githubHome;

    @Column("gitee_home")
    private String giteeHome;

    @Column("csdn_home")
    private String csdnHome;

    @Column("zhihu_home")
    private String zhihuHome;

    @Column("email")
    private String email;

    @Column("seo_title")
    private String seoTitle;// SEO标题

    @Column("seo_keywords")
    private String seoKeywords;// SEO关键词

    @Column("seo_description")
    private String seoDescription;

    @Column("beian_code")
    private String beianCode;

    @Column("beian_link")
    private String beianLink;

    @Column("footer_info")
    private String footerInfo;

    @Column("comment_audit")
    private Integer commentAudit;// 评论审核

    @Column("comment_sensitive_filter")
    private Integer commentSensitiveFilter;// 评论敏感词过滤

    @Column("allow_register")
    private Integer allowRegister;// 是否允许注册

    @Column("upload_max_size")
    private Long uploadMaxSize;

    @Column("upload_allowed_types")
    private String uploadAllowedTypes;// 允许上传的文件类型

    @Column("default_theme")
    private String defaultTheme;// 主题名称

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
