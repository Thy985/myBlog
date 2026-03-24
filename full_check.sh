#!/bin/bash
echo "========================================"
echo "=== 站点全面检测报告 ==="
echo "========================================"

echo ""
echo "【1】检查网站访问"
curl -s -I https://xingchen.help | head -5

echo ""
echo "【2】检查端口监听"
netstat -tlnp | grep -E "80|443|8080|3306|6379"

echo ""
echo "【3】检查 Nginx 状态"
nginx -v 2>&1
systemctl status nginx 2>/dev/null | head -3 || ps aux | grep nginx | grep -v grep | head -2

echo ""
echo "【4】检查后端 Java 服务"
ps aux | grep java | grep -v grep | head -2

echo ""
echo "【5】检查 MySQL"
systemctl status mysql 2>/dev/null | head -3 || systemctl status mysqld 2>/dev/null | head -3

echo ""
echo "【6】检查 Redis"
systemctl status redis 2>/dev/null | head -3 || systemctl status redis-server 2>/dev/null | head -3

echo ""
echo "【7】测试后端 API"
curl -s http://localhost:8080/api/article/list 2>/dev/null | head -c 200

echo ""
echo "【8】测试登录 API"
curl -s https://xingchen.help/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"147258369Thy@\"}" | head -c 300

echo ""
echo "【9】检查前端文件"
ls -la /www/wwwroot/myblog/frontend/ | head -8

echo ""
echo "【10】检查后端文件"
ls -la /www/wwwroot/myblog/backend/ | head -8
