#!/bin/bash
echo "=== 检查 Nginx 配置 ==="
cat /www/server/panel/vhost/nginx/xingchen.help.conf | head -20

echo ""
echo "=== 重启 Nginx ==="
nginx -s stop
sleep 2
nginx

echo ""
echo "=== 检查端口 ==="
netstat -tlnp | grep -E "80|443"
