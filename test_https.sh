#!/bin/bash
echo "=== 通过 HTTPS 测试验证码接口 ==="
curl -s https://xingchen.help/api/verification/send-code -H "Content-Type: application/json" -d "{\"email\":\"test@test.com\",\"type\":\"register\"}"
