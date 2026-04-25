package com.xingchen.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
     * PostgreSQL 数据源属性
     */
    @Bean
    @ConfigurationProperties("spring.datasource.postgresql")
    public DataSourceProperties postgresqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * PostgreSQL 数据源
     * 使用 HikariCP 连接池（Spring Boot 默认）
     */
    @Bean(name = "postgresqlDataSource")
    public DataSource postgresqlDataSource(
            @Qualifier("postgresqlDataSourceProperties") DataSourceProperties properties) {
        log.info("初始化 PostgreSQL 数据源: {}", properties.getUrl());
        DataSource dataSource = properties.initializeDataSourceBuilder()
                .build();
        log.info("PostgreSQL 数据源初始化完成");
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
