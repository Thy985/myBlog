# 🚀 DEPLOYMENT - 部署指南

本文档详细介绍 BlogGrowth AI 的部署方式，包括开发环境、测试环境和生产环境。

## 📋 目录

- [环境要求](#环境要求)
- [开发环境](#开发环境)
- [Docker 部署](#docker-部署)
- [生产环境](#生产环境)
- [运维监控](#运维监控)
- [故障排查](#故障排查)

---

## 环境要求

### 最低要求

| 组件 | 最低 | 推荐 |
|------|------|------|
| CPU | 2 核 | 4 核+ |
| 内存 | 4 GB | 8 GB+ |
| 磁盘 | 20 GB | 50 GB+ |
| MySQL | 8.0 | 8.0 |
| Redis | 6.0 | 7.0 |

### 软件依赖

| 软件 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | 后端运行时 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存/会话 |
| MinIO | 最新版 | 对象存储 |

### 可选组件

| 软件 | 用途 |
|------|------|
| Kafka | 异步消息/事件驱动 |
| Milvus/Weaviate | 向量检索/RAG |
| Nginx | 反向代理/静态资源 |

---

## 开发环境

### 1. 后端配置

```bash
cd backend/src/main/resources

# 复制配置模板
cp application.yaml.example application.yaml
```

编辑 `application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myblog?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379

server:
  port: 8080
```

### 2. 前端配置

```bash
cd front

# 安装依赖
pnpm install

# 创建环境配置
echo "VITE_API_BASE_URL=http://localhost:8080/api" > .env.development
```

### 3. 启动服务

```bash
# 终端 1: 后端
cd backend
mvn spring-boot:run

# 终端 2: 前端
cd front
pnpm dev
```

访问：
- 前端：http://localhost:5173
- 后端 API：http://localhost:8080/api
- Swagger：http://localhost:8080/swagger-ui.html

---

## Docker 部署

### 快速部署（docker-compose）

```bash
cd backend

# 启动所有服务
docker-compose up -d
```

这将自动启动：
- MySQL 8.0
- Redis 7.0
- MinIO（对象存储）
- Spring Boot 后端
- Nginx（反向代理）

### 各服务启动顺序

```
1. MySQL      → 等待就绪（约10秒）
2. Redis     → 等待就绪（约5秒）
3. MinIO     → 等待就绪（约10秒）
4. Backend   → 依赖 MySQL/Redis/MinIO
5. Frontend  → 依赖 Backend API
```

### 手动 Docker 部署

```bash
# 1. 构建后端镜像
cd backend
docker build -t myblog-backend:latest .

# 2. 构建前端镜像
cd ../front
docker build -t myblog-frontend:latest .

# 3. 运行数据库和缓存
docker run -d --name mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=myblog \
  mysql:8.0

docker run -d --name redis -p 6379:6379 redis:7

# 4. 运行后端
docker run -d --name backend \
  -p 8080:8080 \
  --link mysql --link redis \
  -e SPRING_PROFILES_ACTIVE=prod \
  myblog-backend:latest

# 5. 运行前端
docker run -d --name frontend -p 80:80 myblog-frontend:latest
```

---

## 生产环境

### 1. 服务器准备

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com | sudo sh

# 安装 Docker Compose
sudo apt install docker-compose -y

# 配置 Docker 开机启动
sudo systemctl enable docker
```

### 2. 安全配置

```bash
# 配置防火墙
sudo ufw allow 22    # SSH
sudo ufw allow 80     # HTTP
sudo ufw allow 443    # HTTPS
sudo ufw enable

# Docker 安全配置
sudo cat > /etc/docker/daemon.json << EOF
{
  "icc": false,
  "userns-remap": "default",
  "live-restore": true,
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF
```

### 3. SSL 证书（Let's Encrypt）

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx -y

# 获取证书
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# 自动续期
sudo crontab -e
# 添加: 0 0 * * * certbot renew --quiet
```

### 4. Nginx 配置

```nginx
# /etc/nginx/sites-available/myblog
upstream backend {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # 前端静态资源
    location / {
        root /var/www/myblog/front/dist;
        try_files $uri $uri/ /index.html;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # API 代理
    location /api {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }

    # WebSocket
    location /ws {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 5. 启动服务

```bash
# 启用站点
sudo ln -s /etc/nginx/sites-available/myblog /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# 启动应用
cd /opt/myblog
docker-compose -f docker-compose.prod.yml up -d

# 检查状态
docker-compose ps
docker-compose logs -f
```

### 6. 生产环境检查清单

```markdown
## ✅ 部署检查清单

### 安全
- [ ] SSL 证书已配置
- [ ] 防火墙已设置
- [ ] 数据库密码已修改
- [ ] API 密钥已加密存储
- [ ] 敏感文件权限正确

### 性能
- [ ] Redis 缓存已启用
- [ ] CDN 已配置
- [ ] 图片压缩已启用
- [ ] 数据库连接池已调优

### 监控
- [ ] 日志收集已配置
- [ ] 监控告警已设置
- [ ] 备份策略已配置
```

---

## 运维监控

### 日志管理

```bash
# 查看后端日志
docker logs -f myblog-backend

# 查看前端日志
docker logs -f myblog-frontend

# 查看 Nginx 日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### 监控指标

| 指标 | 监控地址 | 说明 |
|------|----------|------|
| JVM | /actuator/metrics/jvm | 内存、GC |
| HTTP | /actuator/metrics/http | 请求量、延迟 |
| 数据库 | Druid 内置 | 连接池状态 |
| Redis | Redis Monitor | 缓存命中率 |

### 备份策略

```bash
# 每日凌晨 3 点备份数据库
0 3 * * * docker exec mysql mysqldump -u root -psecret myblog > /backup/myblog_$(date +\%Y\%m\%d).sql

# 保留最近 30 天备份
find /backup -name "myblog_*.sql" -mtime +30 -delete
```

---

## 故障排查

### 常见问题

#### 1. 后端启动失败

```bash
# 检查日志
docker logs myblog-backend

# 常见原因：
# - 数据库连接失败 → 检查网络和密码
# - 端口被占用 → lsof -i:8080
# - 内存不足 → docker stats
```

#### 2. 前端加载慢

```bash
# 检查是否使用生产构建
docker exec myblog-frontend ls -la /usr/share/nginx/html

# 启用 gzip 压缩
docker exec myblog-frontend nginx -s reload
```

#### 3. 数据库连接超时

```bash
# 检查 MySQL 状态
docker exec mysql mysql -u root -psecret -e "SELECT 1"

# 检查连接数
docker exec mysql mysql -u root -psecret -e "SHOW PROCESSLIST"
```

### 紧急回滚

```bash
# 回滚到上一个版本
docker pull thy985/myblog:previous-tag
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d

# 或者使用之前的镜像 tag
docker tag thy985/myblog:previous-tag thy985/myblog:latest
docker-compose -f docker-compose.prod.yml up -d
```

---

## 📚 相关文档

- [架构文档](ARCHITECTURE.md)
- [贡献指南](CONTRIBUTING.md)
- [变更日志](CHANGELOG.md)
