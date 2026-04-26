# CLAUDE.md - 开发指南

本文件为 Claude Code 提供项目上下文和开发规范。

## 项目概述

**BlogGrowth AI** - AI 驱动的自动增长博客系统

- **后端**: Spring Boot 3.4 + MyBatis-Flex + PostgreSQL (pgvector)
- **前端**: Vue 3 + TypeScript + Element Plus + Vite
- **AI**: LangChain4j 多模型支持 + RAG + 意图分类

## 关键目录

```
backend/
├── src/main/java/com/xingchen/backend/
│   ├── ai/              # AI Agent 核心（意图分类、LLM、工具、记忆）
│   ├── controller/      # REST API 控制器
│   ├── service/         # 业务服务层
│   ├── mapper/          # MyBatis-Flex 数据访问
│   ├── config/          # 配置类
│   └── entity/dto/vo/   # 数据模型

front/
├── src/
│   ├── api/             # API 接口 (~25 文件，部分 JS)
│   ├── components/      # Vue 组件 (~70)
│   ├── views/           # 页面 (~32)
│   ├── stores/          # Pinia 状态管理
│   └── composables/     # 组合式函数

docs/
├── ARCHITECTURE.md      # 系统架构文档
├── API接口文档.md        # API 文档
├── DEPLOYMENT.md        # 部署指南
└── myblog-api-openapi.yaml  # OpenAPI 规范
```

## 开发规范

### Git 工作流

- master 分支受保护，需通过 PR 合并
- 创建新分支: `git checkout -b fix/xxx-feature`
- 提交: `git commit -m "fix: description"`

### 环境变量要求

后端必需的环境变量（无默认值）:
- `ENCRYPTION_MASTER_KEY` - 加密主密钥
- `BAIDU_API_KEY` - 百度 API Key
- `TAVILY_API_KEY` - Tavily 搜索 Key

### 安全要求

- 禁止在代码中硬编码密码、API Key
- 使用 `${ENV_VAR}` 引用环境变量
- 敏感信息不写入 Git

### 代码风格

- 后端: Java 17, Spring Boot 3.4, MyBatis-Flex
- 前端: Vue 3 Composition API, TypeScript（逐步迁移）
- 注释: 必要的业务逻辑注释，禁止无意义注释

## AI 模块说明

### 意图分类器

位置: `backend/ai/intent/`

- `HybridIntentClassifier` - 主分类器（@Primary）
- `RegexIntentClassifier` - 正则快速匹配
- `EmbeddingIntentClassifier` - 向量语义匹配
- `PgVectorIntentRepository` - PostgreSQL pgvector 持久化

优先级: Regex > Embedding（使用 PgVector 加速）

### LLM 提供商

位置: `backend/ai/llm/`

支持: OpenAI, GLM, Claude (via OpenRouter), Ollama, Baidu

### 配置阈值

```yaml
ai:
  intent:
    high-confidence-threshold: 0.95  # 高置信度直接返回
    min-confidence-threshold: 0.7     # 低于此值返回 UNKNOWN
```

## 常见问题

### 编译错误

```bash
# 后端
cd backend && mvn compile -q -DskipTests

# 前端
cd front && npm run build
```

### 数据库迁移

PostgreSQL pgvector 用于向量存储，已配置自动初始化。

### 测试

测试文件中的密码从环境变量读取:
- `TEST_ADMIN_PASSWORD`
- `TEST_USER_PASSWORD`