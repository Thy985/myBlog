#!/bin/bash
echo "=== 测试登录API ==="
curl -s https://xingchen.help/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}"
