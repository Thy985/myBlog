#!/bin/bash
echo "=== 检查 Nginx 状态 ==="
systemctl status nginx 2>/dev/null || service nginx status 2>/dev/null || ps aux | grep nginx | grep -v grep

echo ""
echo "=== 测试网站访问 ==="
curl -v https://xingchen.help 2>&1 | head -30
