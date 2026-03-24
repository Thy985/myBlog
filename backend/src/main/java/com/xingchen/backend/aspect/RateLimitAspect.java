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
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 环绕通知方法，实现基于令牌桶算法的限流控制
     * 
     * @param point 连接点对象，包含被拦截方法的信息
     * @param rateLimit 限流注解对象，包含限流配置参数
     * @return 方法执行结果或限流错误响应
     * @throws Throwable 方法执行过程中可能抛出的异常
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        // 获取客户端请求信息
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String clientId = IpUtils.getClientIp(request);
        String key = rateLimit.key() + ":" + clientId;

        // 获取或创建令牌桶，并尝试消费一个令牌
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(rateLimit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // 根据令牌消费结果决定是否允许执行
        if (probe.isConsumed()) {
            return point.proceed();
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("限流触发 - Key: {}, 客户端: {}, 需等待: {}秒", key, clientId, waitForRefill);
            return Result.error(429, rateLimit.message());
        }
    }

    /**
     * 创建令牌桶实例，根据限流配置选择不同的限流策略
     * 
     * @param rateLimit 限流注解对象，包含限流配置参数
     * @return 配置好的令牌桶实例
     */
    private Bucket createBucket(RateLimit rateLimit) {
        // 按分钟限流策略：使用贪婪填充模式
        if (rateLimit.perMinute() > 0) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(rateLimit.perMinute(), Refill.greedy(rateLimit.perMinute(), Duration.ofMinutes(1))))
                    .build();
        }
        // 自定义时间窗口限流策略：使用间隔填充模式
        return Bucket.builder()
                .addLimit(Bandwidth.classic(rateLimit.capacity(), Refill.intervally(rateLimit.capacity(), Duration.ofSeconds(rateLimit.timeWindow()))))
                .build();
    }
}
