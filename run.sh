#!/bin/bash
pkill -f "backend-0.0.1-SNAPSHOT.jar" || true
sleep 3
cd /www/wwwroot/myblog/backend
nohup java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > backend.log 2>&1 &
sleep 40
echo "=== 测试验证码接口 ==="
curl -s http://localhost:8080/api/verification/send-code -H "Content-Type: application/json" -d "{\"email\":\"3881736197@qq.com\",\"type\":\"register\"}"
