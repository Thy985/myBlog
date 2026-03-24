#!/bin/bash
echo "=== 检查宝塔数据库配置 ==="
cat /www/server/panel/class/common.py | grep -i "mysql\|database" | head -20

echo ""
echo "=== 检查宝塔配置 ==="
ls -la /www/server/panel/config/

echo ""
echo "=== 检查宝塔连接设置 ==="
cat /www/server/panel/config/stop.pl 2>/dev/null | head -10
