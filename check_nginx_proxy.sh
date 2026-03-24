#!/bin/bash
echo "=== 检查 Nginx 代理配置 ==="
ls -la /etc/nginx/conf.d/ 2>/dev/null || echo "无conf.d目录"

echo ""
echo "=== 检查 bt 相关的 proxy 配置 ==="
cat /etc/nginx/nginx.conf | grep -i "proxy" | head -10

echo ""
echo "=== 查看宝塔端口配置 ==="
cat /www/server/panel/vhost/nginx/*.conf 2>/dev/null | grep -i "8888\|proxy" | head -10
