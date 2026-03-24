#!/bin/bash
echo "=== 测试登录 API ==="

# 直接访问后端
echo "【直接后端】"
curl -s http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}"

echo ""
echo ""
echo "【通过 Nginx (HTTPS)】"
curl -s -k https://xingchen.help/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}"
