#!/bin/bash
echo "=== 检查 Nginx 配置 ==="
nginx -t

echo ""
echo "=== 重载 Nginx ==="
nginx -s reload

echo ""
echo "=== 检查端口监听 ==="
netstat -tlnp | grep -E "80|443"

echo ""
echo "=== 测试访问 ==="
curl -s http://127.0.0.1 | head -5
