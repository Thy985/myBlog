#!/bin/bash
echo "=== 尝试使用 bt 命令行工具添加站点 ==="

# 使用宝塔的命令行工具
bt 13

echo ""
echo "=== 检查 nginx 配置文件 ==="
ls -la /www/server/panel/vhost/nginx/xingchen.help.conf

echo ""
echo "=== 测试网站访问 ==="
curl -s -I https://xingchen.help | head -5
