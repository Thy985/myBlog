#!/bin/bash
echo "=== 查看 MySQL 中的所有数据库 ==="
mysql -uroot -p147258369Thy@ -e "SHOW DATABASES"

echo ""
echo "=== 查看 MySQL 中的 bt 相关表 ==="
mysql -uroot -p147258369Thy@ -e "SHOW TABLES" blog 2>/dev/null || echo "blog数据库不存在"

echo ""
echo "=== 尝试查找站点信息 ==="
mysql -uroot -p147258369Thy@ -e "SHOW TABLES" 2>/dev/null | head -20
