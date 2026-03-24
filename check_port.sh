#!/bin/bash
echo "=== 检查端口占用 ==="
netstat -tlnp | grep -E "8888|80|443"

echo ""
echo "=== 检查宝塔服务 ==="
ps aux | grep -i "bt-panel\|panel" | grep -v grep

echo ""
echo "=== 尝试直接访问宝塔 ==="
curl -s http://127.0.0.1:8888/ | head -5

echo ""
echo "=== 尝试使用本地 socket ==="
ls -la /www/server/panel/data/ | head -10
