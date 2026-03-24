package com.xingchen.backend.exception;

import cn.dev33.satoken.exception.*;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.security.UserRateLimitAspect;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



/**
 * 全局异常处理器
 * 统一处理各类异常，返回标准化错误响应
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return Result.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        return Result.fail(400, message);
    }

    /**
     * 安全异常
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<Void> handleSecurityException(SaTokenException e) {
        log.warn("安全异常: {}", e.getMessage());
        return Result.fail(403, "请求不安全: " + e.getMessage());
    }

    /**
     * 限流异常
     */
    @ExceptionHandler({RequestNotPermitted.class, UserRateLimitAspect.RateLimitException.class})
    public Result<Void> handleRateLimitException(Exception e) {
        log.warn("限流触发: {}", e.getMessage());
        return Result.fail(429, "请求过于频繁，请稍后再试");
    }

    /**
     * 熔断异常
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public Result<Void> handleCircuitBreakerException(CallNotPermittedException e) {
        log.warn("熔断触发: {}", e.getMessage());
        return Result.fail(503, "服务暂时不可用，请稍后再试");
    }

    /**
     * Sa-Token 认证异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("未登录: {}", e.getMessage());
        return Result.fail(401, "请先登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("无权限: {}", e.getMessage());
        return Result.fail(403, "无权限访问");
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("无角色: {}", e.getMessage());
        return Result.fail(403, "无角色权限");
    }

    /**
     * 其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}