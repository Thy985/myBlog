#!/bin/bash
echo "=== 查看 site.db ==="
ls -la /www/server/panel/data/site.db

echo ""
echo "=== 查看数据库文件列表 ==="
ls -la /www/server/panel/data/

echo ""
echo "=== 查看所有表 ==="
sqlite3 /www/server/panel/data/site.db ".tables" 2>/dev/null || echo "site.db无法读取"
