#!/bin/bash
echo "=== 查看 site.db 中的站点 ==="
sqlite3 /www/server/panel/data/site.db "SELECT * FROM sites"

echo ""
echo "=== 查看 site.db 表结构 ==="
sqlite3 /www/server/panel/data/site.db ".tables"

echo ""
echo "=== 查看 sites 表内容 ==="
sqlite3 /www/server/panel/data/site.db "SELECT id,name,domain,path,status FROM sites"
