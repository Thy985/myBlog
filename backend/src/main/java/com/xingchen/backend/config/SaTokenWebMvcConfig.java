package com.xingchen.backend.config;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenWebMvcConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    public SaTokenConfig saTokenConfig() {
        return new SaTokenConfig()
                .setTokenName("X-Auth-Token")
                .setActiveTimeout(7 * 24 * 60 * 60)
                .setTimeout(7 * 24 * 60 * 60)
                .setIsConcurrent(true)
                .setIsShare(true)
                .setIsReadCookie(true)
                .setIsReadHeader(true)
                .setIsReadBody(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/api/auth/**",
                        "/api/captcha/**",
                        "/captcha/**",
                        "/verification/**",
                        "/password/**",
                        "/user/register",
                        "/login",
                        "/admin/login",
                        "/error",
                        "/api/health",
                        "/api/blog/setting/**",
                        "/api/tag/**",
                        "/api/category/**",
                        "/api/article/{id}",
                        "/api/article/list",
                        "/api/article/user/{userId}",
                        "/api/article/hot",
                        "/api/article/archive",
                        "/api/article/{id}/stats",
                        "/api/article/related",
                        "/api/article/search",
                        "/api/search/**",
                        "/api/comment/list",
                        "/api/comment/{rootId}/replies",
                        "/api/growth/cycles",
                        "/uploads/**"
                );
    }
}
