#!/bin/bash
echo "=== 检查 Redis 状态 ==="
redis-cli -h 127.0.0.1 -p 6379 -a 147258369Thy@ ping

echo ""
echo "=== 检查后端日志 ==="
tail -30 /www/wwwroot/myblog/backend/backend.log
