package com.xingchen.backend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private String nickname;
    private String intro;
    private String bio;
    private String website;
    private String location;
    private String github;
    private String gitee;
    private String csdn;
    private String zhihu;
    private Integer gender;
    private LocalDate birthday;
    private Integer status;
    private Integer mfaEnabled;
    private String mfaType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}