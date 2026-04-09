package com.xingchen.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

public class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    // 可信代理 IP 列表（仅这些 IP 的 X-Forwarded-For 才可信）
    private static final String[] TRUSTED_PROXY_IPS = {
        "127.0.0.1",
        "0:0:0:0:0:0:0:1",
        "::1"
    };

    // IP 地址格式正则（IPv4 和 IPv6）
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
        "^::1$|" +
        "^::$|" +
        "^([0-9a-fA-F]{1,4}:)*:[0-9a-fA-F]{1,4}$"
    );

    private IpUtils() {
    }

    /**
     * 获取客户端真实 IP 地址
     * 仅在请求来自可信代理时信任 X-Forwarded-For 等 header
     */
    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean isFromTrustedProxy = isTrustedProxy(remoteAddr);

        String ip = null;

        if (isFromTrustedProxy) {
            // 仅在来自可信代理时，才尝试从 header 获取真实 IP
            ip = getHeaderValue(request, "X-Forwarded-For");
            if (!isValidIp(ip)) {
                ip = getHeaderValue(request, "Proxy-Client-IP");
            }
            if (!isValidIp(ip)) {
                ip = getHeaderValue(request, "WL-Proxy-Client-IP");
            }
            if (!isValidIp(ip)) {
                ip = getHeaderValue(request, "HTTP_CLIENT_IP");
            }
            if (!isValidIp(ip)) {
                ip = getHeaderValue(request, "HTTP_X_FORWARDED_FOR");
            }
        }

        // 如果 header 中没有有效 IP，则使用 remoteAddr
        if (!isValidIp(ip)) {
            ip = remoteAddr;
        }

        // 取第一个 IP（多级代理时）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 标准化 IPv6 环回地址
        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IP;
        }

        return ip;
    }

    /**
     * 判断请求是否来自可信代理
     */
    private static boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) return false;
        for (String trusted : TRUSTED_PROXY_IPS) {
            if (trusted.equals(remoteAddr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 header 获取值，过滤 UNKNOWN
     */
    private static String getHeaderValue(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value == null || value.isEmpty() || UNKNOWN.equalsIgnoreCase(value)) {
            return null;
        }
        // 去除空格
        value = value.trim();
        // X-Forwarded-For 可能包含多个 IP，取第一个（真实用户 IP）
        if (value.contains(",")) {
            value = value.split(",")[0].trim();
        }
        return value;
    }

    /**
     * 验证 IP 地址格式是否合法
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        // 去除空格
        ip = ip.trim();
        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
    }
}
