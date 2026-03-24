package com.xingchen.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置类
 * 配置静态资源映射，支持文件上传访问
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 映射到文件系统路径
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);  // 缓存1小时
        
        System.out.println("静态资源映射已配置: /uploads/** -> " + uploadDir);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 使用Nginx反向代理，同源访问，不需要CORS配置
    }
}
