package com.xingchen.backend.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PostgreSQL 数据源配置
 * 用于向量存储、全文搜索等 PostgreSQL 特有功能
 */
@Configuration
@Slf4j
public class PostgreSQLConfig {

    /**
     * PostgreSQL 数据源
     * 使用 Druid 连接池
     */
    @Bean(name = "postgresqlDataSource")
    public DataSource postgresqlDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/myblog");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin123");
        dataSource.setInitialSize(5);
        dataSource.setMaxActive(20);
        dataSource.setMinIdle(5);
        dataSource.setMaxWait(30000);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(100);

        log.info("初始化 PostgreSQL 数据源: {}", dataSource.getUrl());
        return dataSource;
    }

    /**
     * PostgreSQL JdbcTemplate
     * 用于执行原生 SQL 和调用 PostgreSQL 特有函数
     */
    @Bean(name = "postgresqlJdbcTemplate")
    public JdbcTemplate postgresqlJdbcTemplate(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
