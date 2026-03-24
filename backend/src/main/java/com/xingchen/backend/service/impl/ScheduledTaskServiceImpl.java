package com.xingchen.backend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.ScheduledTask;
import com.xingchen.backend.mapper.ScheduledTaskMapper;
import com.xingchen.backend.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    private final ScheduledTaskMapper scheduledTaskMapper;

    @Override
    public List<ScheduledTask> getByUserId(Long userId) {
        return scheduledTaskMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(ScheduledTask::getUserId).eq(userId)
        );
    }

    @Override
    public ScheduledTask getById(Long id) {
        return scheduledTaskMapper.selectOneById(id);
    }

    @Override
    public ScheduledTask create(ScheduledTask task) {
        task.setStatus("ACTIVE");
        task.setRunCount(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        if (task.getCronExpression() != null) {
            calculateNextRunTime(task);
        }
        
        scheduledTaskMapper.insert(task);
        return task;
    }

    @Override
    public ScheduledTask update(ScheduledTask task) {
        task.setUpdateTime(LocalDateTime.now());
        
        if (task.getCronExpression() != null) {
            calculateNextRunTime(task);
        }
        
        scheduledTaskMapper.update(task);
        return task;
    }

    @Override
    public void delete(Long id) {
        scheduledTaskMapper.deleteById(id);
    }

    @Override
    public void runNow(Long id) {
        ScheduledTask task = getById(id);
        if (task != null) {
            task.setLastRunTime(LocalDateTime.now());
            task.setRunCount(task.getRunCount() + 1);
            calculateNextRunTime(task);
            task.setUpdateTime(LocalDateTime.now());
            scheduledTaskMapper.update(task);
            
            log.info("手动执行任务: {}", task.getTaskName());
        }
    }

    @Override
    public void pause(Long id) {
        ScheduledTask task = getById(id);
        if (task != null) {
            task.setStatus("PAUSED");
            task.setUpdateTime(LocalDateTime.now());
            scheduledTaskMapper.update(task);
        }
    }

    @Override
    public void resume(Long id) {
        ScheduledTask task = getById(id);
        if (task != null) {
            task.setStatus("ACTIVE");
            calculateNextRunTime(task);
            task.setUpdateTime(LocalDateTime.now());
            scheduledTaskMapper.update(task);
        }
    }

    private void calculateNextRunTime(ScheduledTask task) {
        try {
            CronExpression cron = CronExpression.parse(task.getCronExpression());
            LocalDateTime next = cron.next(LocalDateTime.now());
            task.setNextRunTime(next);
        } catch (Exception e) {
            log.error("解析Cron表达式失败: {}", task.getCronExpression(), e);
            task.setNextRunTime(null);
        }
    }
}
