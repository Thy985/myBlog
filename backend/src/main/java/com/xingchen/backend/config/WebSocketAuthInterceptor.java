package com.xingchen.backend.config;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 鉴权拦截器
 * 在握手阶段验证用户身份
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                // 从 URL 参数或 Header 中获取 token
                String token = extractToken(servletRequest);
                
                if (token == null || token.isEmpty()) {
                    log.warn("WebSocket 握手失败：缺少认证 token");
                    return false;
                }

                // 验证 token
                Object loginId = StpUtil.getLoginIdDefaultNull();
                if (loginId == null) {
                    log.warn("WebSocket 握手失败：无效的 token");
                    return false;
                }

                // 将用户信息存入 attributes
                attributes.put("userId", loginId.toString());
                attributes.put("token", token);
                
                log.debug("WebSocket 鉴权成功，用户: {}", loginId);
                return true;
            }
        } catch (Exception e) {
            log.error("WebSocket 鉴权异常", e);
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后的处理
    }

    private String extractToken(ServletServerHttpRequest request) {
        // 优先从 URL 参数获取
        String token = request.getServletRequest().getParameter("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }

        // 从 Header 获取
        String authHeader = request.getServletRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}