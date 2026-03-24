#!/bin/bash
echo "=== 查看数据库文件 ==="
ls -la /www/server/panel/data/*.db

echo ""
echo "=== 检查 SQLite 版本 ==="
sqlite3 --version

echo ""
echo "=== 查看数据库表 ==="
sqlite3 /www/server/panel/data/bt_default.db ".tables"

echo ""
echo "=== 查询 sites 表 ==="
sqlite3 /www/server/panel/data/bt_default.db "SELECT id,name,status FROM sites LIMIT 10"
