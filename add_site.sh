#!/bin/bash
BT_USER="f0735fd0"
BT_PASS="f95b4c235814"

echo "=== 获取宝塔 Token ==="
TOKEN=$(curl -s -X POST "http://localhost:8888/login" \
  -d "username=$BT_USER&password=$BT_PASS" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "获取Token失败，尝试其他方式..."
  exit 1
fi

echo "Token: $TOKEN"

echo ""
echo "=== 添加站点 xingchen.help ==="
RESULT=$(curl -s "http://localhost:8888/sites?action=add_site" \
  -b "bt_user=$BT_USER;bt_token=$TOKEN" \
  -d "webname=xingchen.help&domain=xingchen.help&root=/www/wwwroot/myblog/frontend&ftp=false&sql=false&type=php&port=80&phpver=00")

echo "$RESULT"
