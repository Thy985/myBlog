package com.xingchen.backend.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 前端路由转发控制器
 * 处理所有非API路由，支持前端SPA应用
 */
@Controller
public class ForwardController implements ErrorController {

    /**
     * 处理404错误，将前端路由请求转发到index.html
     * 仅处理非API请求和静态资源请求
     */
    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        
        // 如果是API请求，返回JSON格式的404错误
        if (requestUri.startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"code\":404,\"message\":\"API not found\"}");
        }
        
        // 如果是静态资源请求，让Spring处理
        if (requestUri.startsWith("/uploads/") || 
            requestUri.startsWith("/static/") ||
            requestUri.matches(".*\\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot|html)$")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // 前端路由请求，转发到index.html
        return "forward:/index.html";
    }
}
