package com.xingchen.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 全局配置
 * 
 * 统一配置 ObjectMapper，确保所有注入的 ObjectMapper 都具有一致的行为：
 * 1. 支持 Java 8 时间类型（Instant、LocalDateTime 等）
 * 2. 安全的多态类型验证（防止 RCE 攻击）
 * 3. 统一的可见性策略
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置全局 ObjectMapper
     * 
     * @Primary 确保这个 ObjectMapper 被优先注入
     * 所有通过 @Autowired 注入的 ObjectMapper 都会使用这个配置
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // 1. 安全的多态类型验证器
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.xingchen.backend")
                .allowIfSubType("com.xingchen.backend")
                .allowIfBaseType("java.util")
                .allowIfBaseType("java.lang")
                .allowIfBaseType("java.time")
                .allowIfBaseType("java.math")
                .allowIfBaseType("java.net")
                .build();

        ObjectMapper mapper = new ObjectMapper();

        // 2. 配置可见性
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 3. 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());

        // 4. 禁用默认类型信息（避免序列化时添加 @type 字段）
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}
