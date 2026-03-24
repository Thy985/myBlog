#!/bin/bash
echo "=== 修复 blog 用户权限 ==="

mysql -uroot -p147258369Thy@ <<EOF
-- 为 127.0.0.1 创建用户并授权
CREATE USER IF NOT EXISTS 'blog'@'127.0.0.1' IDENTIFIED BY '147258369Thy@';
GRANT ALL PRIVILEGES ON blog.* TO 'blog'@'127.0.0.1';

-- 为 % (任意主机) 创建用户并授权  
CREATE USER IF NOT EXISTS 'blog'@'%' IDENTIFIED BY '147258369Thy@';
GRANT ALL PRIVILEGES ON blog.* TO 'blog'@'%';

FLUSH PRIVILEGES;
EOF

echo ""
echo "=== 验证权限 ==="
mysql -uroot -p147258369Thy@ -e "SHOW GRANTS FOR 'blog'@'localhost'"
mysql -uroot -p147258369Thy@ -e "SHOW GRANTS FOR 'blog'@'127.0.0.1'"
mysql -uroot -p147258369Thy@ -e "SHOW GRANTS FOR 'blog'@'%'"

echo ""
echo "=== 测试连接 ==="
mysql -ublog -p147258369Thy@ -e "SELECT '连接成功' as result"
