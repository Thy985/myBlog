#!/bin/bash
echo "=== 获取宝塔 API 配置 ==="
cat /www/server/panel/config/api.json

echo ""
echo "=== 检查站点数据库 ==="
ls -la /www/server/panel/data/*.db
