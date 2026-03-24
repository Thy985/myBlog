package com.xingchen.backend.config;

import cn.dev33.satoken.stp.StpInterface;
import com.xingchen.backend.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SaTokenStpInterfaceImpl implements StpInterface {

    private final UserRoleMapper userRoleMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Long userId = Long.valueOf(loginId.toString());
            List<String> roles = userRoleMapper.selectRoleCodesByUserId(userId);
            return roles != null ? roles : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}
