#!/bin/bash
echo "=== 最终验证 ==="

echo "【1】网站访问测试"
curl -s -I https://xingchen.help | head -5

echo ""
echo "【2】登录 API 测试"
curl -s https://xingchen.help/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}"

echo ""
echo "【3】后端 API 测试"
curl -s http://localhost:8080/api/article/list | head -c 200
