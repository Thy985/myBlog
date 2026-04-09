package com.xingchen.backend.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant 向量数据库配置
 */
@Configuration
@Slf4j
public class QdrantConfig {

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6334}")
    private int port;

    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(host, port, false);
        QdrantClient client = new QdrantClient(grpcClientBuilder.build());
        log.info("Qdrant 客户端初始化完成: {}:{}", host, port);
        return client;
    }
}