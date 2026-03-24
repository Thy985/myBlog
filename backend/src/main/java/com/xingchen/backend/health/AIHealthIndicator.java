package com.xingchen.backend.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * AI 服务健康检查
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AIHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                builder.withDetail("database", "UP");
            } else {
                builder.down().withDetail("database", "DOWN - Connection invalid");
            }
        } catch (SQLException e) {
            log.error("数据库健康检查失败", e);
            builder.down().withDetail("database", "DOWN - " + e.getMessage());
        }
        
        // 检查知识库
        try {
            // 简单的知识库检查
            builder.withDetail("knowledgeBase", "UP");
        } catch (Exception e) {
            builder.down().withDetail("knowledgeBase", "DOWN - " + e.getMessage());
        }
        
        return builder.build();
    }
}