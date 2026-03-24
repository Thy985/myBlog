package com.xingchen.backend.common;

public enum ErrorCode {
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(1003, "邮箱已被注册"),
    INVALID_PASSWORD(1004, "密码错误"),
    USER_DISABLED(1005, "用户已被禁用"),
    ORIGINAL_PASSWORD_ERROR(1006, "原密码错误"),
    
    ARTICLE_NOT_FOUND(2001, "文章不存在"),
    NO_PERMISSION(2002, "无权限操作"),
    ARTICLE_DELETE_NO_PERMISSION(2003, "无权限删除该文章"),
    ARTICLE_UPDATE_NO_PERMISSION(2004, "无权限修改该文章"),
    
    CATEGORY_NOT_FOUND(3001, "分类不存在"),
    CATEGORY_IN_USE(3002, "分类正在使用中，无法删除"),
    
    TAG_NOT_FOUND(4001, "标签不存在"),
    
    FILE_NOT_FOUND(5001, "文件不存在"),
    FILE_TYPE_NOT_ALLOWED(5002, "不允许上传该类型的文件"),
    FILE_TOO_LARGE(5003, "文件大小超过限制"),
    FILE_CONTENT_MISMATCH(5004, "文件内容与扩展名不匹配"),
    FILE_SAVE_ERROR(5005, "文件保存失败"),
    
    COMMENT_NOT_FOUND(6001, "评论不存在"),
    COMMENT_DELETE_NO_PERMISSION(6002, "无权限删除该评论"),
    COMMENT_DISABLED(6003, "评论已关闭"),

    MFA_ALREADY_ENABLED(7001, "MFA已启用"),
    MFA_NOT_ENABLED(7002, "MFA未启用"),

    NOT_FOUND(8001, "资源不存在"),

    PARAMS_ERROR(9001, "参数错误"),

    SYSTEM_ERROR(9999, "系统繁忙，请稍后重试");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
