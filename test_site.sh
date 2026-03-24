#!/bin/bash
echo "=== 测试网站访问 ==="
curl -s -I https://xingchen.help

echo ""
echo "=== 检查前端文件 ==="
ls -la /www/wwwroot/myblog/frontend/ | head -10
