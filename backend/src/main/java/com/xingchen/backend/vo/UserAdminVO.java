package com.xingchen.backend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserAdminVO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private String nickname;
    private String intro;
    private Integer status;
    private Integer mfaEnabled;
    private String mfaType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
    private List<String> roles; // 用户角色列表
}
