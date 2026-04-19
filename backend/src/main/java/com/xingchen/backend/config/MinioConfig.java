package com.xingchen.backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置
 */
@Configuration
@ConfigurationProperties(prefix = "file.minio")
@Data
@Slf4j
public class MinioConfig {

    /**
     * MinIO 服务地址
     */
    private String endpoint = "http://localhost:9000";

    /**
     * Access Key（用户名）
     */
    private String accessKey = "minioadmin";

    /**
     * Secret Key（密码）
     */
    private String secretKey = "minioadmin";

    /**
     * Bucket 名称
     */
    private String bucket = "myblog";

    /**
     * 是否启用 HTTPS
     */
    private boolean secure = false;

    @Bean
    public MinioClient minioClient() {
        log.info("初始化 MinIO 客户端: endpoint={}, bucket={}", endpoint, bucket);

        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // 尝试创建 bucket（如果不存在）
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("创建 MinIO Bucket 成功: {}", bucket);
            } else {
                log.info("MinIO Bucket 已存在: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("初始化 MinIO Bucket 失败: {}, 将在首次上传时重试", e.getMessage());
        }

        return minioClient;
    }
}
