#!/bin/bash
echo "=== 详细检测 ==="

echo ""
echo "【1】端口检测"
netstat -tlnp | grep -E "80|443"

echo ""
echo "【2】HTTPS 测试"
curl -s -k https://127.0.0.1 | head -c 100

echo ""
echo "【3】公网 HTTPS 测试"
curl -s -k https://xingchen.help | head -c 100

echo ""
echo "【4】登录 API 测试"
curl -s -k https://xingchen.help/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}"

echo ""
echo "【5】数据库连接测试"
mysql -ublog -p147258369Thy@ -e "SELECT '数据库连接正常' as result"

echo ""
echo "【6】后端日志错误检查"
tail -20 /www/wwwroot/myblog/backend/backend.log
