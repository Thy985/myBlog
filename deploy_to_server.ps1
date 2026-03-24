# 部署脚本 - 使用Posh-SSH连接服务器

$ErrorActionPreference = "Stop"

# 服务器信息
$server = "39.106.46.109"
$username = "root"
$password = "147258369Thy@" | ConvertTo-SecureString -AsPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential($username, $password)

# 1. 首先打包本地项目
Write-Host "========== 1. 打包后端项目 ==========" -ForegroundColor Cyan
Set-Location "d:\study\front-end\Myblog\backend"
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "后端打包失败" }
Write-Host "后端打包完成!" -ForegroundColor Green

Write-Host "========== 2. 打包前端项目 ==========" -ForegroundColor Cyan
Set-Location "d:\study\front-end\Myblog\front"
npm run build
if ($LASTEXITCODE -ne 0) { throw "前端打包失败" }
Write-Host "前端打包完成!" -ForegroundColor Green

# 3. 连接服务器并部署
Write-Host "========== 3. 连接服务器 ==========" -ForegroundColor Cyan
$session = New-SSHSession -ComputerName $server -Credential $credential -AcceptKey

Write-Host "连接成功! 检查服务器环境..." -ForegroundColor Green

# 检查Java
$result = Invoke-SSHCommand -SessionId $session.SessionId -Command "java -version 2>&1 | head -1"
Write-Host "Java版本: $($result.Output)"

# 检查MySQL
$result = Invoke-SSHCommand -SessionId $session.SessionId -Command "mysql --version"
Write-Host "MySQL版本: $($result.Output)"

# 停止旧服务
Write-Host "停止旧的后端服务..." -ForegroundColor Yellow
Invoke-SSHCommand -SessionId $session.SessionId -Command "pkill -f 'backend.*jar' || true"

# 上传后端jar包
Write-Host "上传后端jar包..." -ForegroundColor Cyan
$jarPath = "d:\study\front-end\Myblog\backend\target\backend-0.0.1-SNAPSHOT.jar"
Set-SCPItem -SessionId $session.SessionId -LocalFile $jarPath -RemotePath "/root/backend.jar"

# 上传前端
Write-Host "上传前端dist文件夹..." -ForegroundColor Cyan
$distPath = "d:\study\front-end\Myblog\front\dist"
Get-ChildItem $distPath -Recurse | ForEach-Object {
    $remotePath = "/root/dist/" + $_.FullName.Replace($distPath, "").Replace("\", "/")
    if (-not $_.PSIsContainer) {
        Set-SCPItem -SessionId $session.SessionId -LocalFile $_.FullName -RemotePath $remotePath
    }
}

# 创建上传脚本并执行
Write-Host "启动后端服务..." -ForegroundColor Cyan
$startCmd = @"
cd /root
nohup java -jar -Xms512m -Xmx1024m backend.jar --spring.profiles.active=prod > /root/app.log 2>&1 &
sleep 5
if ps -p \$! > /dev/null; then
    echo 'Backend started successfully'
else
    echo 'Backend failed to start'
    cat /root/app.log
fi
"@

Invoke-SSHCommand -SessionId $session.SessionId -Command $startCmd

# 检查启动结果
Start-Sleep -Seconds 10
$healthCheck = Invoke-SSHCommand -SessionId $session.SessionId -Command "curl -s http://localhost:8080/api/health"
Write-Host "健康检查结果: $healthCheck" -ForegroundColor Cyan

# 断开连接
Remove-SSHSession -SessionId $session.SessionId

Write-Host "========== 部署完成! ==========" -ForegroundColor Green
Write-Host "后端地址: http://39.106.46.109:8080" -ForegroundColor Cyan
