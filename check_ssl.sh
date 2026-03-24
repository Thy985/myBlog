#!/bin/bash
echo "=== 检查 Nginx 配置 ==="
cat /www/server/panel/vhost/nginx/xingchen.help.conf

echo ""
echo "=== 检查 SSL 配置 ==="
ls -la /www/server/panel/vhost/ssl/xingchen.help/
