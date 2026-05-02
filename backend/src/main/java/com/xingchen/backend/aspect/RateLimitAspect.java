package com.xingchen.backend.aspect;

import com.xingchen.backend.common.Result;
import com.xingchen.backend.util.IpUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private static final String RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local current = tonumber(redis.call('GET', key) or '0')

        if current >= limit then
            local ttl = redis.call('TTL', key)
            return {0, ttl >= 0 and ttl or window}
        end

        redis.call('INCR', key)
        if current == 0 then
            redis.call('EXPIRE', key, window)
        end

        return {1, 0}
        """;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private volatile boolean useRedis = false;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String clientId = IpUtils.getClientIp(request);
        String key = RATE_LIMIT_PREFIX + rateLimit.key() + ":" + clientId;

        if (useRedis && redisTemplate != null) {
            return handleRedisRateLimit(point, rateLimit, key);
        } else {
            return handleLocalRateLimit(point, rateLimit, key);
        }
    }

    private Object handleRedisRateLimit(ProceedingJoinPoint point, RateLimit rateLimit, String key) throws Throwable {
        try {
            DefaultRedisScript<java.util.List> script = new DefaultRedisScript<>();
            script.setScriptText(RATE_LIMIT_SCRIPT);
            script.setResultType(java.util.List.class);

            int capacity = rateLimit.capacity() > 0 ? rateLimit.capacity() : rateLimit.perMinute();
            int window = rateLimit.timeWindow() > 0 ? rateLimit.timeWindow() : 60;

            java.util.List<?> result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(window)
            );

            if (result != null && result.size() >= 2) {
                Object first = result.get(0);
                Object second = result.get(1);

                if (first instanceof Number && second instanceof Number) {
                    long allowed = ((Number) first).longValue();
                    long waitSeconds = ((Number) second).longValue();

                    if (allowed == 1) {
                        return point.proceed();
                    } else {
                        log.warn("Redis限流触发 - Key: {}, 需等待: {}秒", key, waitSeconds);
                        return Result.error(429, rateLimit.message());
                    }
                }
            }

            log.warn("Redis限流脚本返回格式异常，降级到本地限流");
            return handleLocalRateLimit(point, rateLimit, key);
        } catch (Exception e) {
            log.error("Redis限流异常，降级到本地限流: {}", e.getMessage());
            useRedis = false;
            return handleLocalRateLimit(point, rateLimit, key);
        }
    }

    private Object handleLocalRateLimit(ProceedingJoinPoint point, RateLimit rateLimit, String key) throws Throwable {
        Bucket bucket = localBuckets.computeIfAbsent(key, k -> createBucket(rateLimit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return point.proceed();
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("本地限流触发 - Key: {}, 客户端: {}, 需等待: {}秒", key, rateLimit.key(), waitForRefill);
            return Result.error(429, rateLimit.message());
        }
    }

    private Bucket createBucket(RateLimit rateLimit) {
        if (rateLimit.perMinute() > 0) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(rateLimit.perMinute(), Refill.greedy(rateLimit.perMinute(), Duration.ofMinutes(1))))
                    .build();
        }
        return Bucket.builder()
                .addLimit(Bandwidth.classic(rateLimit.capacity(), Refill.intervally(rateLimit.capacity(), Duration.ofSeconds(rateLimit.timeWindow()))))
                .build();
    }

    @Autowired(required = false)
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        if (redisTemplate != null) {
            this.redisTemplate = redisTemplate;
            this.useRedis = true;
            log.info("分布式限流已启用，使用 Redis 存储");
        }
    }
}
