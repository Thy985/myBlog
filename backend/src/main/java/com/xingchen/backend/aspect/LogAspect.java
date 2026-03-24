package com.xingchen.backend.aspect;

import com.xingchen.backend.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LogAspect {
    // 定义切点，匹配所有控制器方法
    @Pointcut("execution(* com.xingchen.backend.controller..*.*(..))")
    public void controllerPointcut() {}

    /**
     * 环绕通知方法，用于记录控制器方法的执行日志
     * 
     * @param point 连接点对象，包含目标方法的信息
     * @return 目标方法的执行结果
     * @throws Throwable 目标方法可能抛出的异常
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取HTTP请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String ip = IpUtils.getClientIp(request);
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = point.getSignature().getName();

        // 记录请求开始日志
        log.info("[请求开始] {} {} | IP: {} | Class: {}.{} | Args: {}",
                method, uri, ip, className, methodName,
                Arrays.toString(point.getArgs()));

        Object result;
        try {
            result = point.proceed();
        } catch (Exception e) {
            // 记录异常日志并重新抛出
            log.error("[请求异常] {} {} | Error: {}", method, uri, e.getMessage());
            throw e;
        }

        // 记录请求结束日志，包括执行耗时
        long costTime = System.currentTimeMillis() - startTime;
        log.info("[请求结束] {} {} | Cost: {}ms", method, uri, costTime);

        return result;
    }
}
