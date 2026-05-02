
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
│   ├── src/main/java/   # 源代码
│   │   ├── agent/       # AI 编排层（Orchestrator, Session, Workflow）
│   │   ├── ai/          # AI 能力层
│   │   │   ├── intent/  # 意图分类（Regex/Embedding/Hybrid）
│   │   │   ├── llm/     # LLM Provider（OpenAI/GLM/Claude/Baidu/Ollama）
│   │   │   ├── tool/    # 工具系统
│   │   │   ├── memory/  # 三级记忆系统
│   │   │   └── model/   # 数据模型
│   │   ├── controller/  # REST API 控制器
│   │   ├── service/    # 业务服务层
│   │   ├── mapper/     # MyBatis-Flex 数据访问
│   │   ├── entity/dto/vo/  # 数据模型
│   │   ├── config/     # 配置类
│   │   └── vector/     # 向量数据库集成
│   ├── src/main/resources/  # 配置文件
│   └── pom.xml         # Maven 配置
│
├── front/               # 前端项目（Vue 3 + Vite）
│   ├── src/            # 源代码
│   │   ├── api/        # API 接口定义
│   │   ├── components/ # Vue 组件
│   │   │   └── business/agent/  # 智能体 UI 组件
│   │   ├── views/      # 页面（frontend/ & admin/）
│   │   ├── stores/     # Pinia 状态管理
│   │   ├── composables/ # 组合式函数
│   │   ├── layouts/    # 布局组件
│   │   └── router/     # 路由配置
│   └── package.json
│
├── docs/               # 项目文档
│   ├── ARCHITECTURE.md         # 系统架构
│   ├── AGENT_ARCHITECTURE.md   # AI 智能体架构
│   ├── API接口文档.md           # API 文档
│   ├── DEPLOYMENT.md           # 部署指南
│   ├── CHANGELOG.md            # 变更日志
│   └── myblog-api-openapi.yaml # OpenAPI 规范
│
└── README.md           # 项目总览（本文件）
```

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.4.x
- **ORM**: MyBatis-Flex
- **主数据库**: MySQL 8.0
- **向量数据库**: PostgreSQL 15+ (pgvector) / Qdrant
- **缓存**: Redis 6.0+
- **消息队列**: RabbitMQ
- **对象存储**: MinIO
- **邮件**: Spring Mail
- **安全**: Sa-Token
- **监控**: Druid + Actuator + Micrometer
- **AI 框架**: LangChain4j
- **智能体**: 分层编排架构 (AI层 + Agent层)
- **LLM 支持**: GLM / OpenAI / Claude / Baidu / Ollama / OpenRouter

### 前端
- **框架**: Vue 3.3+
- **构建**: Vite 4.x
- **语言**: TypeScript
- **UI**: Element Plus
- **样式**: Tailwind CSS
- **状态**: Pinia
- **路由**: Vue Router 4
- **HTTP**: Axios

## 🚀 快速开始

### 环境要求

- **后端**: JDK 17+, Maven 3.8+, MySQL 8.0+, PostgreSQL 15+, Redis 6.0+
- **前端**: Node.js 18+, pnpm 8+

### 1️⃣ 克隆项目

```bash
git clone https://github.com/Thy985/myBlog.git
cd myBlog
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

- [系统架构](./ARCHITECTURE.md) - 整体架构设计
- [AI 智能体架构](./AGENT_ARCHITECTURE.md) - Agent 核心架构
- [API 接口文档](./API接口文档.md) - REST API 详细说明
- [OpenAPI 规范](./myblog-api-openapi.yaml) - API 规范文件
- [部署指南](./DEPLOYMENT.md) - 开发/生产环境部署
- [变更日志](./CHANGELOG.md) - 版本历史
- [贡献指南](./CONTRIBUTING.md) - 如何参与贡献
- [页面跳转指南](./开发文档/页面跳转指南.md) - 前端路由机制
- [用户使用指南](./开发文档/用户使用指南文档.md) - 用户操作手册

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
- MySQL 8.0 数据库
- PostgreSQL（向量数据 + pgvector）
- Redis 缓存
- MinIO 对象存储
- RabbitMQ 消息队列
- Spring Boot 后端

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
- ✅ **AI 智能体助手** - 悬浮对话面板，支持文章生成、内容优化、智能问答

### 后台功能
- ✅ 文章管理（增删改查）
- ✅ 分类/标签管理
- ✅ 评论管理（审核）
- ✅ 用户管理
- ✅ 系统配置
- ✅ 数据统计
- ✅ **AI 智能体配置** - 模型管理、工具配置、知识库管理

### AI 智能体功能
- ✅ 智能对话 - 自然语言交互，理解用户意图
- ✅ 文章生成 - 根据主题自动生成 SEO 优化文章
- ✅ 内容优化 - 润色、扩写、改写现有文章
- ✅ 工具调用 - 文章查询、编辑、发布、删除等操作
- ✅ 知识检索 - RAG 检索增强，基于知识库回答
- ✅ 多模型支持 - OpenAI、GLM、Baidu、Ollama 等
- ✅ 流式响应 - 实时打字机效果，思考过程可视化
- ✅ 会话管理 - 用户隔离、历史记录、上下文记忆

## 🤝 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📚 文档

- [系统架构文档](./ARCHITECTURE.md)
- [AI 智能体架构](./AGENT_ARCHITECTURE.md)
- [API 接口文档](./API接口文档.md)
- [部署指南](./DEPLOYMENT.md)
- [变更日志](./CHANGELOG.md)

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👤 作者

- **Thy985** - [GitHub](https://github.com/Thy985/myBlog)

## 🙏 致谢

感谢所有贡献者和开源项目的支持！

## 📚 相关文档

- [智能体架构文档](./AGENT_ARCHITECTURE.md) - AI Agent 详细架构设计
- [API 接口文档](./API接口文档.md) - 完整的 API 接口说明
- [部署指南](./DEPLOYMENT.md) - 环境搭建和部署教程
- [系统架构](./ARCHITECTURE.md) - 整体系统架构设计

## 📮 联系方式

- Email: 1850833838@qq.com
- Blog: https://xingchen.cloud
- Issues: https://github.com/Thy985/myBlog/issues

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**
