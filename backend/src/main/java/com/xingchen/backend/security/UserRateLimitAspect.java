package com.xingchen.backend.security;

import cn.dev33.satoken.stp.StpUtil;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 用户级限流切面
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class UserRateLimitAspect {

    private final RateLimitConfig rateLimitConfig;

    @Around("@annotation(com.xingchen.backend.security.annotation.UserRateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Long userId = StpUtil.getLoginIdAsLong();
        
        RateLimiter rateLimiter = rateLimitConfig.getUserRateLimiter(userId);
        
        try {
            return rateLimiter.executeCallable(() -> {
                try {
                    return point.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RequestNotPermitted e) {
            log.warn("用户 {} 请求过于频繁", userId);
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }
    }

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}