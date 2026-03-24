package com.xingchen.backend.service;

import com.xingchen.backend.entity.ScheduledTask;

import java.util.List;

public interface ScheduledTaskService {
    
    List<ScheduledTask> getByUserId(Long userId);
    
    ScheduledTask getById(Long id);
    
    ScheduledTask create(ScheduledTask task);
    
    ScheduledTask update(ScheduledTask task);
    
    void delete(Long id);
    
    void runNow(Long id);
    
    void pause(Long id);
    
    void resume(Long id);
}
