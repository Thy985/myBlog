package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.entity.ScheduledTask;
import com.xingchen.backend.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final ScheduledTaskService scheduledTaskService;

    @SaCheckLogin
    @GetMapping
    public Result<List<ScheduledTask>> getTasks() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(scheduledTaskService.getByUserId(userId));
    }

    @SaCheckLogin
    @GetMapping("/{id}")
    public Result<ScheduledTask> getTask(@PathVariable Long id) {
        return Result.success(scheduledTaskService.getById(id));
    }

    @SaCheckLogin
    @PostMapping
    public Result<ScheduledTask> createTask(@RequestBody ScheduledTask task) {
        Long userId = StpUtil.getLoginIdAsLong();
        task.setUserId(userId);
        return Result.success(scheduledTaskService.create(task));
    }

    @SaCheckLogin
    @PutMapping("/{id}")
    public Result<ScheduledTask> updateTask(@PathVariable Long id, @RequestBody ScheduledTask task) {
        task.setId(id);
        return Result.success(scheduledTaskService.update(task));
    }

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        scheduledTaskService.delete(id);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/{id}/run")
    public Result<Void> runNow(@PathVariable Long id) {
        scheduledTaskService.runNow(id);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/{id}/pause")
    public Result<Void> pause(@PathVariable Long id) {
        scheduledTaskService.pause(id);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id) {
        scheduledTaskService.resume(id);
        return Result.success();
    }
}
