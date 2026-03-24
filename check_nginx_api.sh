#!/bin/bash
echo "=== 检查当前 Nginx 配置 ==="
cat /www/server/panel/vhost/nginx/xingchen.help.conf | grep -A 10 "location"

echo ""
echo "=== 检查 extension 目录 ==="
ls -la /www/server/panel/vhost/nginx/extension/xingchen.help/
