package com.xingchen.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatar;

    @Size(max = 100, message = "个人简介长度不能超过255个字符")
    private String bio;
}
