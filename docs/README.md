
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.3-brightgreen.svg)](https://vuejs.org/)

现代化的个人博客系统，采用前后端分离架构，提供完整的博客管理和展示功能。

## ✨ 特性

- 📝 **文章管理** - 支持 Markdown 编辑、分类、标签、草稿
- 💬 **评论系统** - 多级评论、点赞、审核
- 👥 **用户系统** - 注册登录、角色权限、个人中心
- 🎨 **主题定制** - 响应式设计、暗色模式
- 📊 **数据统计** - 访问量、热门文章、数据看板
- 🔍 **全文搜索** - 快速检索文章内容
- 🚀 **性能优化** - Redis 缓存、CDN 加速
- 🐳 **容器化部署** - Docker 一键部署

## 🏗️ 项目结构

```
Myblog/
├── backend/              # 后端项目（Spring Boot）
│   ├── module-admin/    # 后台管理模块
│   ├── module-common/   # 公共模块（实体、工具类）
│   ├── web/            # 前台展示模块
│   ├── sql/            # 数据库脚本
│   ├── .env.example    # 环境变量模板
│   ├── pom.xml         # Maven 配置
│   └── README.md       # 后端文档
│
├── front/               # 前端项目（Vue 3 + Vite）
│   ├── src/            # 源代码
│   │   ├── api/        # API 接口
│   │   ├── components/ # 组件
│   │   ├── views/      # 页面
│   │   ├── stores/     # 状态管理
│   │   └── router/     # 路由
│   ├── .env.example    # 环境变量模板
│   └── README.md       # 前端文档
│
├── docs/               # 项目文档
│   ├── 开发文档/       # 开发指南
│   ├── 设计文档/       # 设计文档
│   ├── 配置文档/       # 配置说明
│   └── 整理报告/       # 优化报告
│
└── README.md           # 项目总览（本文件）
```

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 2.7
- **ORM**: MyBatis-Flex
- **数据库**: MySQL 8.0
- **缓存**: Redis 5.0+
- **对象存储**: MinIO
- **邮件**: Spring Mail
- **安全**: Spring Security + JWT
- **监控**: Druid + Actuator

### 前端
- **框架**: Vue 3.3
- **构建**: Vite 4.x
- **语言**: TypeScript
- **UI**: Element Plus
- **样式**: Tailwind CSS
- **状态**: Pinia
- **路由**: Vue Router 4
- **HTTP**: Axios

## 🚀 快速开始

### 环境要求

- **后端**: JDK 11+, Maven 3.6+, MySQL 8.0+, Redis 5.0+
- **前端**: Node.js 16+, npm 8+ 或 pnpm 7+

### 1️⃣ 克隆项目

```bash
git clone https://github.com/yourusername/myblog.git
cd myblog
```

### 2️⃣ 后端配置

```bash
cd backend

# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，填入数据库、Redis 等配置

# 2. 初始化数据库
mysql -u root -p < sql/init.sql

# 3. 启动服务（方式一：本地运行）
mvn clean install
mvn spring-boot:run

# 或（方式二：Docker 运行）
docker-compose up -d
```

后端服务启动后访问：
- API 地址: http://localhost:8080/api
- Swagger 文档: http://localhost:8080/swagger-ui.html
- Druid 监控: http://localhost:8080/druid

### 3️⃣ 前端配置

```bash
cd front

# 1. 安装依赖
npm install
# 或使用 pnpm（推荐）
pnpm install

# 2. 配置环境变量
cp .env.example .env.development
# 编辑 .env.development

# 3. 启动开发服务器
npm run dev
```

前端服务启动后访问：http://localhost:5173

## 📖 详细文档

- [后端开发文档](backend/README.md)
- [前端开发文档](front/README.md)
- [项目结构最佳实践](docs/整理报告/项目结构最佳实践建议.md)
- [快速开始指南](docs/开发文档/Myblog快速开始指南.md)
- [API 接口文档](docs/myblog-api-openapi.yaml)

## 📦 Git 提交规范

本项目采用 Monorepo 结构，提交时请遵循以下规范：

### 提交内容

✅ **应该提交：**
- `backend/` - 后端源码（排除 .env、target/）
- `front/` - 前端源码（排除 .env.*、node_modules/、dist/）
- `docs/` - 项目文档
- `README.md` - 项目说明
- `.env.example` - 环境变量模板

❌ **不要提交：**
- `.env` - 环境变量（包含敏感信息）
- `node_modules/` - 前端依赖
- `target/` - 后端构建产物
- `dist/` - 前端构建产物
- `.idea/`、`.vscode/` - IDE 配置

### 提交信息格式

```
<type>(<scope>): <subject>

类型（type）：
- feat: 新功能
- fix: 修复 Bug
- docs: 文档更新
- style: 代码格式（不影响功能）
- refactor: 重构
- perf: 性能优化
- test: 测试
- chore: 构建/工具变动

范围（scope）：
- backend: 后端
- frontend: 前端
- docs: 文档
- all: 全部

示例：
feat(backend): 添加文章评论功能
fix(frontend): 修复登录页面样式问题
docs: 更新部署文档
```

### Git 工作流

```bash
# 1. 创建功能分支
git checkout -b feature/article-comment

# 2. 开发并提交
git add .
git commit -m "feat(backend): 添加文章评论功能"

# 3. 推送到远程
git push origin feature/article-comment

# 4. 创建 Pull Request 合并到 develop

# 5. 发布时从 develop 合并到 main
```

## 🐳 Docker 部署

### 快速部署（推荐）

```bash
# 在项目根目录执行
cd backend
docker-compose up -d
```

这将启动：
- MySQL 数据库
- Redis 缓存
- MinIO 对象存储
- Spring Boot 应用

### 分别部署

```bash
# 后端
cd backend
docker build -t myblog-backend .
docker run -d -p 8080:8080 --env-file .env myblog-backend

# 前端
cd front
docker build -t myblog-frontend .
docker run -d -p 80:80 myblog-frontend
```

## 📊 功能模块

### 前台功能
- ✅ 首页文章列表
- ✅ 文章详情页
- ✅ 分类/标签筛选
- ✅ 文章搜索
- ✅ 评论系统
- ✅ 用户注册/登录
- ✅ 个人中心

### 后台功能
- ✅ 文章管理（增删改查）
- ✅ 分类/标签管理
- ✅ 评论管理（审核）
- ✅ 用户管理
- ✅ 系统配置
- ✅ 数据统计

## 🤝 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👤 作者

- **Your Name** - [GitHub](https://github.com/yourusername)

## 🙏 致谢

感谢所有贡献者和开源项目的支持！

## 📮 联系方式

- Email: your.email@example.com
- Blog: https://yourblog.com
- Issues: https://github.com/yourusername/myblog/issues

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**
