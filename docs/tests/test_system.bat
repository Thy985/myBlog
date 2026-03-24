@echo off
REM Myblog系统自动化测试脚本 (Windows版本)
REM 作者: 小跃AI助手
REM 日期: 2026-02-10

echo =========================================
echo   Myblog系统自动化测试
echo =========================================
echo.

REM 配置
set BASE_URL=http://localhost:8080/api
set USERNAME=admin
set PASSWORD=147258369Thy@
set TOKEN=
set COMMENT_ID=

REM 测试结果统计
set /a TOTAL_TESTS=0
set /a PASSED_TESTS=0
set /a FAILED_TESTS=0

echo =========================================
echo 阶段1: 认证功能测试
echo =========================================
echo.

echo 1.1 用户登录测试
curl -X POST "%BASE_URL%/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"%USERNAME%\",\"password\":\"%PASSWORD%\"}" ^
  -o login_response.json
echo.

echo 1.2 获取用户信息测试
echo 请先从login_response.json中复制token，然后手动测试
echo curl -X GET "%BASE_URL%/auth/info" -H "Authorization: Bearer YOUR_TOKEN"
echo.

echo =========================================
echo 阶段2: 数据库验证
echo =========================================
echo.

echo 2.1 验证登录历史表
echo 请在MySQL中执行:
echo SELECT * FROM t_login_history ORDER BY login_time DESC LIMIT 5;
echo.

echo 2.2 验证敏感词表
echo 请在MySQL中执行:
echo SELECT * FROM t_sensitive_word;
echo.

echo =========================================
echo 测试完成
echo =========================================
echo.
echo 请查看生成的响应文件:
echo - login_response.json
echo.
echo 建议使用Postman导入测试集合:
echo Myblog_Test_Collection.postman_collection.json
echo.

pause
