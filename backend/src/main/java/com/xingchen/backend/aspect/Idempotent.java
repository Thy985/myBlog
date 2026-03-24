package com.xingchen.backend.aspect;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    String key() default "";
    
    int expireSeconds() default 10;
    
    String message() default "操作过于频繁，请稍后重试";
}
