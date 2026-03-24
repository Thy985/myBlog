package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.UserCreateDTO;
import com.xingchen.backend.service.AdminService;
import com.xingchen.backend.vo.UserAdminVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
@Validated
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        return Result.success(adminService.getDashboardData());
    }

    @GetMapping("/users")
    public Result<PageResult<UserAdminVO>> getUserList(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        List<UserAdminVO> list = adminService.getUserList(page, size, keyword, status);
        long total = adminService.countUsers(keyword, status);
        return Result.success(PageResult.of(list, total, page, size));
    }

    @PostMapping("/users")
    public Result<UserAdminVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(adminService.createUser(dto));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        adminService.updateUserStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/users/{id}/roles")
    public Result<Void> assignRoles(
            @PathVariable Long id,
            @RequestBody List<Long> roleIds) {
        adminService.assignRoles(id, roleIds);
        return Result.success();
    }

    @GetMapping("/settings")
    public Result<Map<String, String>> getSettings() {
        return Result.success(adminService.getSettings());
    }

    @PutMapping("/settings/{key}")
    public Result<Void> updateSetting(
            @PathVariable String key,
            @RequestParam String value) {
        adminService.updateSetting(key, value);
        return Result.success();
    }
}
