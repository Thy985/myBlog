#!/bin/bash
echo "=== 停止旧的后端进程 ==="
pkill -f "backend-0.0.1-SNAPSHOT.jar" || true
sleep 3

echo "=== 启动新的后端 ==="
cd /www/wwwroot/myblog/backend

export DB_URL="jdbc:mysql://localhost:3306/blog?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="147258369Thy@"
export REDIS_HOST="127.0.0.1"
export REDIS_PORT="6379"
export REDIS_PASSWORD="147258369Thy@"
export REDIS_DATABASE="0"
export JWT_SECRET="myblog-jwt-secret-key-2024"
export MAIL_HOST="smtp.qq.com"
export MAIL_PORT="587"
export MAIL_USERNAME="xingchen_help"
export MAIL_PASSWORD="coeqawdvwoaidcbe"

nohup java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > backend.log 2>&1 &

echo "=== 等待启动 ==="
sleep 30

echo "=== 测试验证码接口 ==="
curl -s http://localhost:8080/api/verification/send-code -H "Content-Type: application/json" -d "{\"email\":\"test@test.com\",\"type\":\"register\"}"
