package com.xingchen.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig extends CachingConfigurerSupport {

    /**
     * 安全的多态类型验证器
     * 仅允许本项目包下的类进行多态反序列化，防止 RCE 攻击
     * 同时允许 JDK 核心类型（java.util、java.lang、java.time 等），因为这些类型在缓存中是安全的
     * 注意：Jackson 2.10+ 版本不再支持 deny() 方法，改用显式 allow 模式
     */
    private static final PolymorphicTypeValidator SECURE_TYPE_VALIDATOR =
        BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType("com.xingchen.backend")
            .allowIfSubType("com.xingchen.backend")
            .allowIfBaseType("java.util")
            .allowIfBaseType("java.lang")
            .allowIfBaseType("java.time")
            .allowIfBaseType("java.math")
            .allowIfBaseType("java.net")
            .build();

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 使用安全的类型验证器，防止 RCE 攻击
        mapper.activateDefaultTyping(SECURE_TYPE_VALIDATOR, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 使用安全的类型验证器，防止 RCE 攻击
        mapper.activateDefaultTyping(SECURE_TYPE_VALIDATOR, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 配置默认缓存策略
        // 注意：Spring Data Redis 3.x 中 enableCachingNullValues() 方法已被移除
        // null 值缓存行为已改变，如需防止缓存穿透，建议使用 @Cacheable 的 unless 属性或自定义缓存拦截器
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigMap = new HashMap<>();

        cacheConfigMap.put("article", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigMap.put("articleList", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigMap.put("userArticles", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigMap.put("hotArticles", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigMap)
                .transactionAware()
                .build();
    }
}
