package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.service.NotificationService;
import com.xingchen.backend.vo.NotificationVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<PageResult<NotificationVO>> getNotificationList(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码至少为1") Integer page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页至少1条")
            @Max(value = 100, message = "每页最多100条") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<NotificationVO> result = notificationService.getNotificationList(userId, page, size);
        return Result.success(result);
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.deleteNotification(id, userId);
        return Result.success();
    }
}
