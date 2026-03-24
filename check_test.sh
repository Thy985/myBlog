#!/bin/bash
echo "=== 检查后端进程 ==="
ps aux | grep "backend-0.0.1-SNAPSHOT.jar" | grep -v grep

echo ""
echo "=== 测试验证码接口 ==="
curl -s http://localhost:8080/api/verification/send-code -H "Content-Type: application/json" -d "{\"email\":\"test@test.com\",\"type\":\"register\"}"

echo ""
echo "=== 检查后端日志 ==="
tail -30 /www/wwwroot/myblog/backend/backend.log
