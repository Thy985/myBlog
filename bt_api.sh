#!/bin/bash
# 宝塔 API 添加站点

echo "=== 获取宝塔登录信息 ==="
BT_USER="f0735fd0"
BT_PASS="f95b4c235814"

# 获取 session
echo "=== 尝试登录获取 session ==="
LOGIN_RESULT=$(curl -s -X POST "http://localhost:8888/login" \
  -d "username=$BT_USER&password=$BT_PASS")

echo "$LOGIN_RESULT"

# 检查是否登录成功
if echo "$LOGIN_RESULT" | grep -q "status"; then
    echo ""
    echo "=== 登录响应包含 status ==="
fi
