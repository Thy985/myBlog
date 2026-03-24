#!/bin/bash
echo "=== 检查端口 ==="
netstat -tlnp | grep -E "80|443"

echo ""
echo "=== 测试网站 ==="
curl -s -I https://xingchen.help
