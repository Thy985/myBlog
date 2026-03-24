#!/bin/bash
echo "=== 检查当前 Nginx 配置 ==="
ls -la /www/server/panel/vhost/nginx/

echo ""
echo "=== 检查宝塔数据库 ==="
mysql -uroot -p147258369Thy@ bt_panel -e "SELECT * FROM sites" 2>/dev/null || echo "无法访问bt_panel数据库"
