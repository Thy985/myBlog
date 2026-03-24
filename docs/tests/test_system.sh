#!/bin/bash

# Myblog系统自动化测试脚本
# 作者: 小跃AI助手
# 日期: 2026-02-10

echo "========================================="
echo "  Myblog系统自动化测试"
echo "========================================="
echo ""

# 配置
BASE_URL="http://localhost:8080/api"
USERNAME="admin"
PASSWORD="147258369Thy@"
TOKEN=""
USER_ID=""
COMMENT_ID=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_api() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_code=$5
    local headers=$6
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "测试 $TOTAL_TESTS: $test_name ... "
    
    if [ -z "$headers" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method \
            -H "Content-Type: application/json" \
            -H "$headers" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" == "$expected_code" ]; then
        echo -e "${GREEN}✓ 通过${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "$body"
        return 0
    else
        echo -e "${RED}✗ 失败${NC} (期望: $expected_code, 实际: $http_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "$body"
        return 1
    fi
}

# 开始测试
echo "========================================="
echo "阶段1: 认证功能测试"
echo "========================================="
echo ""

# 测试1: 用户登录
echo "1.1 用户登录测试"
response=$(test_api "正常登录" "POST" "/auth/login" \
    "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" "200")

if [ $? -eq 0 ]; then
    # 提取token
    TOKEN=$(echo "$response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    USER_ID=$(echo "$response" | grep -o '"userId":[0-9]*' | cut -d':' -f2)
    echo "Token: $TOKEN"
    echo "User ID: $USER_ID"
fi
echo ""

# 测试2: 获取用户信息
echo "1.2 获取用户信息测试"
test_api "获取用户信息" "GET" "/auth/info" "" "200" "Authorization: Bearer $TOKEN"
echo ""

# 测试3: Token刷新
echo "1.3 Token刷新测试"
test_api "Token刷新" "POST" "/auth/refresh" "" "200" "Authorization: Bearer $TOKEN"
echo ""

# 测试4: 查询登录历史
echo "1.4 查询登录历史测试"
test_api "查询登录历史" "GET" "/auth/login-history?page=1&pageSize=10" "" "200" "Authorization: Bearer $TOKEN"
echo ""

echo "========================================="
echo "阶段2: 用户管理功能测试"
echo "========================================="
echo ""

# 测试5: 获取个人资料
echo "2.1 获取个人资料测试"
test_api "获取个人资料" "GET" "/user/profile" "" "200" "Authorization: Bearer $TOKEN"
echo ""

# 测试6: 更新个人资料
echo "2.2 更新个人资料测试"
test_api "更新个人资料" "PUT" "/user/profile" \
    "{\"nickname\":\"测试昵称\",\"intro\":\"这是测试简介\"}" "200" "Authorization: Bearer $TOKEN"
echo ""

echo "========================================="
echo "阶段3: 评论系统功能测试"
echo "========================================="
echo ""

# 测试7: 发布评论
echo "3.1 发布评论测试"
response=$(test_api "发布评论" "POST" "/comment/publish" \
    "{\"articleId\":1,\"content\":\"这是一条测试评论\",\"parentId\":0}" "200" "Authorization: Bearer $TOKEN")

if [ $? -eq 0 ]; then
    # 提取评论ID
    COMMENT_ID=$(echo "$response" | grep -o '"data":[0-9]*' | cut -d':' -f2)
    echo "Comment ID: $COMMENT_ID"
fi
echo ""

# 测试8: 查询评论列表
echo "3.2 查询评论列表测试"
test_api "查询评论列表" "GET" "/comment/list?articleId=1&page=1&pageSize=10" "" "200" "Authorization: Bearer $TOKEN"
echo ""

# 测试9: 评论点赞
echo "3.3 评论点赞测试"
if [ -n "$COMMENT_ID" ]; then
    test_api "评论点赞" "POST" "/comment/like/$COMMENT_ID" "" "200" "Authorization: Bearer $TOKEN"
else
    echo -e "${YELLOW}跳过（无评论ID）${NC}"
fi
echo ""

# 测试10: 敏感词过滤
echo "3.4 敏感词过滤测试"
test_api "敏感词过滤" "POST" "/comment/publish" \
    "{\"articleId\":1,\"content\":\"这是一条包含垃圾内容的评论\",\"parentId\":0}" "200" "Authorization: Bearer $TOKEN"
echo ""

# 测试11: 删除评论
echo "3.5 删除评论测试"
if [ -n "$COMMENT_ID" ]; then
    test_api "删除评论" "DELETE" "/comment/$COMMENT_ID" "" "200" "Authorization: Bearer $TOKEN"
else
    echo -e "${YELLOW}跳过（无评论ID）${NC}"
fi
echo ""

echo "========================================="
echo "阶段4: 安全性测试"
echo "========================================="
echo ""

# 测试12: 未登录访问受保护接口
echo "4.1 未登录访问测试"
test_api "未登录访问" "GET" "/user/profile" "" "401" ""
echo ""

# 测试13: 无效Token
echo "4.2 无效Token测试"
test_api "无效Token" "GET" "/user/profile" "" "401" "Authorization: Bearer invalid-token"
echo ""

# 测试14: SQL注入测试
echo "4.3 SQL注入测试"
test_api "SQL注入防护" "POST" "/auth/login" \
    "{\"username\":\"admin' OR '1'='1\",\"password\":\"anything\"}" "500" ""
echo ""

echo "========================================="
echo "测试结果汇总"
echo "========================================="
echo ""
echo "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}✗ 部分测试失败，请检查日志${NC}"
    exit 1
fi
