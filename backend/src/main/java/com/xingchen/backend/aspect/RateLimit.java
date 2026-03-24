package com.xingchen.backend.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API限流注解
 */
@Target(ElementType.METHOD)//只能方法上使用
@Retention(RetentionPolicy.RUNTIME)//运行时生效
public @interface RateLimit {
    /**
     * 限流key前缀
     */
    String key() default "default";

    /**
     * 容量（请求数）
     */
    int capacity() default 10;

    /**
     * 时间窗口（秒）
     */
    int timeWindow() default 60;

    /**
     * 每分钟允许的请求数（优先于capacity+timeWindow）
     */
    int perMinute() default 0;

    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后再试";
}
