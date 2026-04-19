package com.xingchen.backend.job;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.mapper.UserApiKeyMapper;
import com.xingchen.backend.service.UserApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApiKeyHealthCheckJob {

    private final UserApiKeyService userApiKeyService;
    private final UserApiKeyMapper userApiKeyMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "healthCheck", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void healthCheck() {
        log.info("开始 API Key 健康检查任务");
        long startTime = System.currentTimeMillis();

        try {
            List<UserApiKey> apiKeys = getActiveApiKeys();

            int totalChecked = 0;
            int invalidCount = 0;

            for (UserApiKey apiKey : apiKeys) {
                totalChecked++;
                boolean isValid = userApiKeyService.isApiKeyValid(apiKey.getUserId());

                if (!isValid) {
                    invalidCount++;
                    log.warn("API Key 无效: userId={}, provider={}",
                            apiKey.getUserId(), apiKey.getProvider());
                    sendNotification(apiKey.getUserId(), apiKey.getProvider());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("API Key 健康检查完成: total={}, invalid={}, duration={}ms",
                    totalChecked, invalidCount, duration);

        } catch (Exception e) {
            log.error("API Key 健康检查失败", e);
        }
    }

    private List<UserApiKey> getActiveApiKeys() {
        LocalDateTime now = LocalDateTime.now();
        return userApiKeyMapper.selectListByQuery(
                QueryWrapper.create()
                        .le("start_time", now)
                        .ge("end_time", now)
        );
    }

    private void sendNotification(Long userId, String provider) {
        log.info("发送 API Key 失效通知: userId={}, provider={}", userId, provider);
    }
}
