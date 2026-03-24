#!/bin/bash
echo "=== 检查后端邮件配置 ==="
grep -i "mail" /www/wwwroot/myblog/backend/backend.log | tail -20

echo ""
echo "=== 检查环境变量中的邮件配置 ==="
env | grep -i "mail"

echo ""
echo "=== 查看最近日志（发送验证码后的）==="
tail -100 /www/wwwroot/myblog/backend/backend.log | grep -i -E "send|mail|email|verification" | tail -20
