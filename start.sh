#!/bin/bash
pkill -f "backend-0.0.1-SNAPSHOT.jar" || true
sleep 3
cd /www/wwwroot/myblog/backend
nohup java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > backend.log 2>&1 &
sleep 40
echo "=== 测试Health接口 ==="
curl -s http://localhost:8080/api/health
