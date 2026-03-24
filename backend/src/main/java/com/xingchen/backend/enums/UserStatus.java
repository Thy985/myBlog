package com.xingchen.backend.enums;

public enum UserStatus {
    DISABLED(0, "inactive", "已禁用"),
    ACTIVE(1, "active", "正常");

    private final int code;
    private final String name;
    private final String description;

    UserStatus(int code, String name, String description) {
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
        if (code == null) return DISABLED.name;
        return code == ACTIVE.code ? ACTIVE.name : DISABLED.name;
    }

    public static int toCode(String name) {
        if (name == null) return DISABLED.code;
        return ACTIVE.name.equalsIgnoreCase(name) ? ACTIVE.code : DISABLED.code;
    }

    public static UserStatus fromCode(Integer code) {
        if (code == null) return DISABLED;
        return code == ACTIVE.code ? ACTIVE : DISABLED;
    }

    public static UserStatus fromName(String name) {
        if (name == null) return DISABLED;
        return ACTIVE.name.equalsIgnoreCase(name) ? ACTIVE : DISABLED;
    }
}
