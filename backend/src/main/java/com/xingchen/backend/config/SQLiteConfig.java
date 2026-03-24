package com.xingchen.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLite 数据库配置
 * 为 MemoryRepository 提供数据库连接
 */
@Configuration
@Slf4j
public class SQLiteConfig {

    @Bean
    public Connection sqliteConnection() {
        try {
            // 确保 SQLite JDBC 驱动已加载
            Class.forName("org.sqlite.JDBC");
            
            // 使用文件数据库，存储在项目目录下
            String dbUrl = "jdbc:sqlite:myblog_memory.db";
            Connection conn = DriverManager.getConnection(dbUrl);
            log.info("SQLite 数据库连接已创建: {}", dbUrl);
            return conn;
        } catch (ClassNotFoundException e) {
            log.error("SQLite JDBC 驱动未找到", e);
            throw new RuntimeException("SQLite JDBC 驱动未找到，请添加依赖", e);
        } catch (SQLException e) {
            log.error("创建 SQLite 连接失败", e);
            throw new RuntimeException("创建 SQLite 连接失败", e);
        }
    }
}
