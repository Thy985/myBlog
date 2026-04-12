package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.PasswordUpdateDTO;
import com.xingchen.backend.dto.UserRegisterDTO;
import com.xingchen.backend.dto.UserUpdateDTO;
import com.xingchen.backend.service.FileService;
import com.xingchen.backend.service.UserService;
import com.xingchen.backend.vo.FileVO;
import com.xingchen.backend.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileService fileService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @GetMapping("/profile")
    @SaCheckLogin
    public Result<UserVO> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.getUserInfo(userId));
    }

    @PutMapping("/profile")
    @SaCheckLogin
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updateUserInfo(userId, dto);
        return Result.success();
    }

    @PutMapping("/password")
    @SaCheckLogin
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updatePassword(userId, dto);
        return Result.success();
    }

    @PostMapping("/avatar")
    @SaCheckLogin
    public Result<FileVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        FileVO fileVO = fileService.uploadAvatar(userId, file);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setAvatar(fileVO.getFileUrl());
        userService.updateUserInfo(userId, dto);

        return Result.success(fileVO);
    }

    @GetMapping("/roles")
    @SaCheckLogin
    public Result<List<String>> getUserRoles() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.getUserRoles(userId));
    }

    @GetMapping("/stats")
    @SaCheckLogin
    public Result<Map<String, Object>> getUserStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.getUserStats(userId));
    }

    @GetMapping("/activities")
    @SaCheckLogin
    public Result<List<Map<String, Object>>> getUserRecentActivities() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.getUserRecentActivities(userId));
    }
}
