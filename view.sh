#!/bin/bash
echo "=== 后端进程 ==="
ps aux | grep "backend-0.0.1-SNAPSHOT.jar" | grep -v grep

echo ""
echo "=== 后端日志 ==="
tail -30 /www/wwwroot/myblog/backend/backend.log
