#!/bin/bash
echo "=== 检查后端日志（验证码相关错误）==="
tail -100 /www/wwwroot/myblog/backend/backend.log | grep -i -E "email|mail|验证码|code|send|fail|error"

echo ""
echo "=== 最近的后端日志 ==="
tail -50 /www/wwwroot/myblog/backend/backend.log
