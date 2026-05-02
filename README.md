# 🚀 BlogGrowth AI - AI驱动自动增长博客

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.3-brightgreen.svg)](https://vuejs.org/)
[![AI Agent](https://img.shields.io/badge/AI-Agent-Enabled-green.svg)]()

> 数据驱动 + AI执行 = 自动增长引擎

**BlogGrowth AI** 是一个AI驱动的自动增长博客系统。它不仅是一个博客，更是一个能够**自动分析用户需求、自动生成内容、自动优化SEO、自动提升流量**的智能系统。

## 🎯 核心价值

| 能力 | 说明 |
|------|------|
| 🔍 **自动发现内容机会** | 分析用户搜索行为，发现网站缺失的内容 |
| ✍️ **自动生成文章** | 基于需求缺口和网站内容，AI自动生成高质量文章 |
| 📈 **自动优化旧内容** | 识别低效文章，自动优化标题、内容、SEO |
| 🎯 **自动做SEO** | 智能关键词挖掘、标题优化、结构优化 |
| 💬 **智能问答** | 用博客内容回答用户问题，提升留存 |

## 🔄 增长闭环

```
[用户行为数据] ──→ [分析 Agent] ──→ [机会识别]
                                            ↓
[发布渠道] ←── [SEO Agent] ←── [写作 Agent] ←─┘
     ↓
[流量增长] ───────────────────────────→ [反馈闭环]
```

## ✨ 功能特性

### 📊 数据驱动层
- **用户行为采集** - PV/UV/停留时间/跳出率/搜索词
- **内容分析** - 热门文章/阅读趋势/用户偏好
- **机会识别** - 发现"用户搜什么但你没有"

### 🤖 AI 智能体层
- **分析 Agent** - 理解用户需求，识别增长机会
- **写作 Agent** - 生成高质量、符合SEO的文章
- **SEO Agent** - 优化关键词、标题、结构
- **问答 Agent** - 用博客内容智能回答用户问题

### 📝 内容管理层
- **文章管理** - Markdown编辑、分类、标签、草稿
- **多模型支持** - OpenAI/Claude/GLM/文心/Ollama
- **记忆管理** - 工作记忆/短期记忆/长期记忆
- **工具调用** - RAG查询/文章CRUD/分类管理

### 🏗️ 基础架构
- **前后端分离** - Spring Boot 3.4 + Vue 3 + Vite
- **向量检索** - PostgreSQL pgvector / Qdrant
- **熔断保护** - Resilience4j 保障系统稳定
- **异步消息** - RabbitMQ 事件驱动

## 🏗️ 项目结构

```
Myblog/
├── backend/                    # 后端 (Spring Boot 3.4)
│   ├── src/main/java/com/xingchen/backend/
│   │   ├── ai/               # AI 智能体核心
│   │   │   ├── orchestrator/ # 编排器
│   │   │   ├── intent/        # 意图分类
│   │   │   ├── llm/          # LLM 提供商
│   │   │   ├── memory/       # 记忆管理
│   │   │   ├── tool/         # 工具集
│   │   │   └── model/        # 数据模型
│   │   ├── controller/       # API 控制器
│   │   ├── service/          # 业务服务
│   │   ├── mapper/          # 数据访问
│   │   └── entity/          # 实体类
│   └── src/main/resources/
│       └── application*.yaml # 配置文件
│
├── front/                      # 前端 (Vue 3 + TypeScript)
│   └── src/
│       ├── components/        # 组件
│       ├── views/             # 页面
│       ├── api/              # API 接口
│       ├── stores/           # 状态管理
│       └── router/           # 路由
│
└── docs/                       # 文档
    ├── ARCHITECTURE.md        # 架构文档
    ├── CONTRIBUTING.md        # 贡献指南
    ├── CHANGELOG.md          # 变更日志
    └── DEPLOYMENT.md         # 部署指南
```

## 🛠️ 技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.x | Web 框架 |
| MyBatis-Flex | 1.11.x | ORM |
| MySQL | 8.0 | 主数据库 |
| PostgreSQL | 15+ | 向量数据库 (pgvector) |
| Sa-Token | 1.38.x | 认证授权 |
| Redis | 6.0+ | 缓存 |
| MinIO | - | 对象存储 |
| RabbitMQ | - | 消息队列 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.3+ | 框架 |
| Vite | 4.x | 构建工具 |
| TypeScript | - | 语言 |
| Element Plus | - | UI 组件 |
| Pinia | - | 状态管理 |
| Tailwind CSS | - | 样式 |

### AI 基础设施
| 技术 | 用途 |
|------|------|
| LangChain4j | Agent 框架 |
| PostgreSQL pgvector / Qdrant | 向量数据库 |
| Tavily API | 联网搜索 |

## 🚀 快速开始

### 环境要求

- **后端**: JDK 17+, Maven 3.8+, MySQL 8.0+, PostgreSQL 15+, Redis 6.0+
- **前端**: Node.js 18+, pnpm 8+
- **可选**: Qdrant (向量数据库), RabbitMQ, MinIO

### 1️⃣ 克隆项目

```bash
git clone https://github.com/Thy985/myBlog.git
cd myBlog
```

### 2️⃣ 后端启动

```bash
cd backend

# 配置环境变量 - 复制并编辑
cp src/main/resources/application-dev.yaml.example src/main/resources/application-dev.yaml
# 或直接编辑 src/main/resources/application-dev.yaml 填入数据库、Redis 等配置

# 启动
mvn spring-boot:run
```

后端启动后：
- API: http://localhost:8080/api
- Swagger: http://localhost:8080/swagger-ui.html

### 3️⃣ 前端启动

```bash
cd front

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev
```

前端启动后访问：http://localhost:5173

## 📖 文档

- [架构文档](docs/ARCHITECTURE.md) - 系统架构详解
- [贡献指南](docs/CONTRIBUTING.md) - 如何参与贡献
- [变更日志](docs/CHANGELOG.md) - 版本历史
- [部署指南](docs/DEPLOYMENT.md) - 生产环境部署

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 `git checkout -b feature/AmazingFeature`
3. 提交更改 `git commit -m 'feat: Add some AmazingFeature'`
4. 推送分支 `git push origin feature/AmazingFeature`
5. 创建 Pull Request

详细指南请阅读 [CONTRIBUTING.md](docs/CONTRIBUTING.md)

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE)

## 👤 作者

- **Thy985** - [GitHub](https://github.com/Thy985)

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**
