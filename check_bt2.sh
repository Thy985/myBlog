#!/bin/bash
echo "=== 检查 default.db ==="
sqlite3 /www/server/panel/data/default.db ".tables"

echo ""
echo "=== 查询站点 ==="
sqlite3 /www/server/panel/data/default.db "SELECT * FROM sites" 2>/dev/null || echo "无sites表"

echo ""
echo "=== 检查 system.db ==="
sqlite3 /www/server/panel/data/system.db ".tables"
