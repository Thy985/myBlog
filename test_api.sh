#!/bin/bash
echo "=== 测试验证码接口 ==="
curl -s http://localhost:8080/api/verification/send-code -H "Content-Type: application/json" -d "{\"email\":\"test@test.com\",\"type\":\"register\"}"

echo ""
echo "=== 测试登录接口 ==="
curl -s http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}"
