#!/bin/bash
echo "=== 查看 MySQL 数据库 ==="
mysql -uroot -p147258369Thy@ -e "SHOW DATABASES"

echo ""
echo "=== 查看数据库用户 ==="
mysql -uroot -p147258369Thy@ -e "SELECT user,host FROM mysql.user"

echo ""
echo "=== 检查 blog 用户 ==="
mysql -uroot -p147258369Thy@ -e "SHOW GRANTS FOR 'blog'@'localhost'"
