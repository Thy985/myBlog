# 部署脚本 - 使用Posh-SSH连接服务器

$ErrorActionPreference = "Stop"

# 服务器信息
$server = "39.106.46.109"
$username = "root"
$password = ConvertTo-SecureString "147258369Thy@" -AsPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential($username, $password)

# 3. 连接服务器并部署
Write-Host "========== 连接服务器 ==========" -ForegroundColor Cyan

try {
    $session = New-SSHSession -ComputerName $server -Credential $credential -AcceptKey -ErrorAction Stop
    Write-Host "连接成功! 检查服务器环境..." -ForegroundColor Green
} catch {
    Write-Host "连接失败: $_" -ForegroundColor Red
    exit 1
}

# 检查Java
$result = Invoke-SSHCommand -SessionId $session.SessionId -Command "java -version 2>&1 | head -1"
Write-Host "Java版本: $($result.Output)"

# 检查MySQL
$result = Invoke-SSHCommand -SessionId $session.SessionId -Command "mysql --version"
Write-Host "MySQL版本: $($result.Output)"

# 停止旧服务
Write-Host "停止旧的后端服务..." -ForegroundColor Yellow
Invoke-SSHCommand -SessionId $session.SessionId -Command "pkill -f 'backend.*jar' || true" -ErrorAction SilentlyContinue

# 上传后端jar包
Write-Host "上传后端jar包..." -ForegroundColor Cyan
$jarPath = "d:\study\front-end\Myblog\backend\target\backend-0.0.1-SNAPSHOT.jar"
Set-SCPItem -SessionId $session.SessionId -LocalFile $jarPath -RemotePath "/root/backend.jar" -Force

# 创建启动脚本
Write-Host "启动后端服务..." -ForegroundColor Cyan
$startCmd = @"
cd /root
nohup java -jar -Xms512m -Xmx1024m backend.jar --spring.profiles.active=prod > /root/app.log 2>&1 &
sleep 8
if ps -p \$! > /dev/null 2>&1; then
    echo 'Backend started successfully'
else
    echo 'Backend failed to start'
    tail -50 /root/app.log
fi
"@

Invoke-SSHCommand -SessionId $session.SessionId -Command $startCmd

# 检查启动结果
Start-Sleep -Seconds 5
$healthCheck = Invoke-SSHCommand -SessionId $session.SessionId -Command "curl -s http://localhost:8080/api/health 2>&1"
Write-Host "健康检查结果: $healthCheck" -ForegroundColor Cyan

# 检查日志
$logCheck = Invoke-SSHCommand -SessionId $session.SessionId -Command "tail -30 /root/app.log 2>&1"
Write-Host "最近日志: $($logCheck.Output)" -ForegroundColor Cyan

# 断开连接
Remove-SSHSession -SessionId $session.SessionId

Write-Host "========== 部署完成! ==========" -ForegroundColor Green
Write-Host "后端地址: http://39.106.46.109:8080" -ForegroundColor Cyan
