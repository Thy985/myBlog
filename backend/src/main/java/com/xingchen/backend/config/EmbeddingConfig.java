package com.xingchen.backend.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Random;

/**
 * Embedding 模型配置
 */
@Configuration
@Slf4j
public class EmbeddingConfig {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        try {
            // 使用本地 BGE-Small-ZH 模型
            log.info("使用本地 BGE-Small-ZH embedding 模型");
            return new BgeSmallZhEmbeddingModel();
        } catch (Exception e) {
            log.warn("无法加载本地 Embedding 模型，使用 Mock 实现: {}", e.getMessage());
            return new MockEmbeddingModel();
        }
    }

    /**
     * Mock Embedding 模型（当本地模型无法加载时使用）
     */
    public static class MockEmbeddingModel implements EmbeddingModel {
        private final Random random = new Random(42);
        private static final int VECTOR_SIZE = 512;

        @Override
        public Response<Embedding> embed(String text) {
            float[] vector = new float[VECTOR_SIZE];
            for (int i = 0; i < VECTOR_SIZE; i++) {
                vector[i] = random.nextFloat();
            }
            return Response.from(Embedding.from(vector));
        }

        @Override
        public Response<Embedding> embed(dev.langchain4j.data.segment.TextSegment textSegment) {
            return embed(textSegment.text());
        }

        @Override
        public Response<List<Embedding>> embedAll(List<dev.langchain4j.data.segment.TextSegment> textSegments) {
            List<Embedding> embeddings = textSegments.stream()
                    .map(ts -> embed(ts).content())
                    .toList();
            return Response.from(embeddings);
        }

        @Override
        public int dimension() {
            return VECTOR_SIZE;
        }
    }
}
