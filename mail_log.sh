#!/bin/bash
echo "=== 检查邮件发送日志 ==="
tail -20 /www/wwwroot/myblog/backend/backend.log | grep -i -E "mail|send|verification|success"
