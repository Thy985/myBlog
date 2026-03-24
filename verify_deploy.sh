#!/bin/bash
echo "=== 验证网站 ==="

echo "【1】网站访问"
curl -s -I https://xingchen.help | head -3

echo ""
echo "【2】登录测试"
curl -s https://xingchen.help/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}" | head -c 200
