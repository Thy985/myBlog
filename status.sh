#!/bin/bash
echo "=== 检查后端进程 ==="
ps aux | grep "backend-0.0.1-SNAPSHOT.jar" | grep -v grep

echo ""
echo "=== 检查后端日志 ==="
tail -50 /www/wwwroot/myblog/backend/backend.log | grep -i -E "started|error|verification|mail"
