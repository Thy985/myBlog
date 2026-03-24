package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_comment")
public class Comment {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("article_id")
    private Long articleId;

    @Column("user_id")
    private Long userId;

    @Column("parent_id")
    private Long parentId; // 父评论ID，0表示顶级评论

    @Column("root_id")
    private Long rootId; // 根评论ID，用于查询所有回复

    @Column("reply_to_user_id")
    private Long replyToUserId; // 回复的用户ID

    @Column("content")
    private String content;

    @Column("level")
    private Integer level; // 评论层级：1-顶级，2-二级，3-三级

    @Column("status")
    private Integer status; // 状态：0-待审核，1-已发布，2-已删除，3-已屏蔽

    @Column("like_count")
    private Integer likeCount;

    @Column("reply_count")
    private Integer replyCount;

    @Column("ip_address")
    private String ipAddress;

    @Column("user_agent")
    private String userAgent;

    @Column("device_type")
    private String deviceType;

    @Column("audit_time")
    private LocalDateTime auditTime;// 审核时间

    @Column("audit_user_id")
    private Long auditUserId;// 审核人ID

    @Column("audit_remark")
    private String auditRemark;// 审核备注

    @Column("is_deleted")
    private Integer isDeleted;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
