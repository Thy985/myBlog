#!/bin/bash
echo "=== 最终验证 ==="

echo "【1】登录 API 测试"
curl -s https://xingchen.help/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}"

echo ""
echo "【2】文章列表 API"
curl -s https://xingchen.help/api/article/list | head -c 200
