#!/bin/bash
echo "=== 检查宝塔数据目录 ==="
ls -la /www/server/panel/data/

echo ""
echo "=== 检查宝塔 SQLite 数据库 ==="
file /www/server/panel/data/*.db

echo ""
echo "=== 尝试读取 sites 表 ==="
sqlite3 /www/server/panel/data/bt_default.db "SELECT * FROM sites" 2>/dev/null || echo "无法读取sites表"
