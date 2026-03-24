#!/bin/bash
echo "=== 测试 Nginx 配置 ==="
nginx -t

echo ""
echo "=== 重载 Nginx ==="
nginx -s reload

echo ""
echo "=== 检查端口 ==="
netstat -tlnp | grep -E "80|443"

echo ""
echo "=== 测试网站 ==="
curl -s -I https://xingchen.help
