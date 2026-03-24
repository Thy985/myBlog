package com.xingchen.backend.service;

import com.xingchen.backend.dto.LoginDTO;
import com.xingchen.backend.dto.PasswordUpdateDTO;
import com.xingchen.backend.dto.UserRegisterDTO;
import com.xingchen.backend.dto.UserUpdateDTO;
import com.xingchen.backend.vo.UserVO;

import java.util.List;
import java.util.Map;

public interface UserService {
    Map<String, Object> login(LoginDTO dto, String ip, String device);

    void logout(String token);

    Map<String, Object> refreshToken(String token);

    UserVO getUserInfo(Long userId);

    List<String> getUserRoles(Long userId);

    Object register(UserRegisterDTO dto);

    void updateUserInfo(Long userId, UserUpdateDTO dto);

    void updatePassword(Long userId, PasswordUpdateDTO dto);

    Map<String, Object> getUserStats(Long userId);

    List<Map<String, Object>> getUserRecentActivities(Long userId);
}
