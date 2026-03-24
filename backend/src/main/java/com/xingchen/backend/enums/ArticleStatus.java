package com.xingchen.backend.enums;

public enum ArticleStatus {
    DRAFT(0, "draft", "草稿"),
    PUBLISHED(1, "published", "已发布");

    private final int code;
    private final String name;
    private final String description;

    ArticleStatus(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static String toName(Integer code) {
        if (code == null) return DRAFT.name;
        return code == PUBLISHED.code ? PUBLISHED.name : DRAFT.name;
    }

    public static int toCode(String name) {
        if (name == null) return DRAFT.code;
        return PUBLISHED.name.equalsIgnoreCase(name) ? PUBLISHED.code : DRAFT.code;
    }

    public static ArticleStatus fromCode(Integer code) {
        if (code == null) return DRAFT;
        return code == PUBLISHED.code ? PUBLISHED : DRAFT;
    }

    public static ArticleStatus fromName(String name) {
        if (name == null) return DRAFT;
        return PUBLISHED.name.equalsIgnoreCase(name) ? PUBLISHED : DRAFT;
    }
}
