#!/bin/bash
echo "=== 尝试使用宝塔命令行工具 ==="
bt 1

echo ""
echo "=== 直接检查 SQLite 数据库 ==="
sqlite3 /www/server/panel/data/bt_default.db "SELECT name FROM sqlite_master WHERE type='table'"

echo ""
echo "=== 查看 sites 表结构 ==="
sqlite3 /www/server/panel/data/bt_default.db ".schema sites" 2>/dev/null || echo "sites表不存在"

echo ""
echo "=== 查看现有站点 ==="
sqlite3 /www/server/panel/data/bt_default.db "SELECT * FROM sites" 2>/dev/null || echo "无法查询sites表"
