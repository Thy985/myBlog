@echo off
echo ==================== 部署脚本 ====================
echo.

echo [1/4] 检查文件...
if not exist "backend\target\backend-0.0.1-SNAPSHOT.jar" (
    echo 错误: 后端jar包不存在，请先运行 mvn clean package
    pause
    exit /b 1
)

if not exist "front\dist" (
    echo 错误: 前端dist不存在，请先运行 npm run build
    pause
    exit /b 1
)

echo [2/4] 上传后端jar包到服务器...
scp backend\target\backend-0.0.1-SNAPSHOT.jar root@39.106.46.109:/root/backend.jar
if errorlevel 1 (
    echo 上传失败，请检查网络连接
    pause
    exit /b 1
)
echo 上传成功!

echo [3/4] 在服务器上启动后端...
ssh root@39.106.46.109 "pkill -f 'backend.*jar' 2>/dev/null; cd /root; nohup java -jar -Xms512m -Xmx1024m backend.jar --spring.profiles.active=prod > app.log 2>&1 &"
echo 启动命令已发送，等待启动...
timeout /t 15 /nobreak >nul

echo [4/4] 检查服务状态...
ssh root@39.106.46.109 "curl -s http://localhost:8080/api/health"
echo.
echo ==================== 部署完成 ====================
echo 后端地址: http://39.106.46.109:8080
pause
