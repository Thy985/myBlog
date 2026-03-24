#!/bin/bash
echo "=== 修复 blog 用户权限 ==="

# 设置 blog 用户密码并授权
mysql -uroot -p147258369Thy@ <<EOF
-- 删除可能存在的用户
DROP USER IF EXISTS 'blog'@'127.0.0.1';
DROP USER IF EXISTS 'blog'@'%';

-- 创建用户并授权
CREATE USER 'blog'@'localhost' IDENTIFIED BY '147258369Thy@';
CREATE USER 'blog'@'127.0.0.1' IDENTIFIED BY '147258369Thy@';
CREATE USER 'blog'@'%' IDENTIFIED BY '147258369Thy@';

-- 授权
GRANT ALL PRIVILEGES ON blog.* TO 'blog'@'localhost';
GRANT ALL PRIVILEGES ON blog.* TO 'blog'@'127.0.0.1';
GRANT ALL PRIVILEGES ON blog.* TO 'blog'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
EOF

echo ""
echo "=== 验证权限 ==="
mysql -uroot -p147258369Thy@ -e "SHOW GRANTS FOR 'blog'@'localhost'"
mysql -uroot -p147258369Thy@ -e "SHOW GRANTS FOR 'blog'@'127.0.0.1'"

echo ""
echo "=== 测试连接 ==="
mysql -ublog -p147258369Thy@ -e "SELECT '连接成功' as result"
