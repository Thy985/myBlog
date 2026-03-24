package com.xingchen.backend.service;

import com.xingchen.backend.dto.UserCreateDTO;
import com.xingchen.backend.vo.UserAdminVO;

import java.util.List;
import java.util.Map;

public interface AdminService {
    Map<String, Object> getDashboardData();

    List<UserAdminVO> getUserList(Integer page, Integer size, String keyword, String status);

    long countUsers(String keyword, String status);

    UserAdminVO createUser(UserCreateDTO dto);

    void updateUserStatus(Long id, String status);

    void deleteUser(Long id);

    void assignRoles(Long id, List<Long> roleIds);

    Map<String, String> getSettings();

    void updateSetting(String key, String value);
}
