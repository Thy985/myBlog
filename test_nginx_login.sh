#!/bin/bash
echo "=== 测试通过Nginx登录 ==="
curl -k -s https://xingchen.help/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}"
