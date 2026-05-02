# 🏗️ ARCHITECTURE - 系统架构文档

本文档详细介绍 BlogGrowth AI 的系统架构，包括整体架构、模块设计、数据流和技术选型。

## 📋 目录

- [架构概览](#架构概览)
- [核心模块](#核心模块)
- [数据流设计](#数据流设计)
- [技术选型](#技术选型)
- [扩展性设计](#扩展性设计)
- [安全设计](#安全设计)

---

## 架构概览

### 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户层 (User Layer)                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Web UI    │  │  Mobile    │  │    API     │              │
│  │  (Vue 3)    │  │  (响应式)   │  │  (REST)    │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
└─────────┼────────────────┼────────────────┼─────────────────────┘
          │                │                │
          ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      网关层 (Gateway Layer)                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Nginx / Spring Gateway                       │   │
│  │         路由 · 限流 · 认证 · SSL · WSS                    │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      应用层 (Application Layer)                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   前台      │  │   管理后台   │  │   AI Agent  │              │
│  │  Blog UI    │  │  Admin UI   │  │   服务      │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
│         │                │                │                     │
│  ┌──────┴────────────────┴────────────────┴────────────────┐    │
│  │                   Controller 层                          │    │
│  │  Article | Category | Tag | User | Analytics | Agent    │    │
│  └──────────────────────────┬───────────────────────────────┘    │
│                             │                                    │
│  ┌──────────────────────────┴───────────────────────────────┐    │
│  │                    Service 层                            │    │
│  │        业务逻辑 · 事务管理 · 权限控制                     │    │
│  └──────────────────────────┬───────────────────────────────┘    │
└─────────────────────────────┼───────────────────────────────────┘
                              │
┌─────────────────────────────┼───────────────────────────────────┐
│                      基础设施层 (Infrastructure Layer)            │
│                             │                                    │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐│
│  │  MySQL  │  │  Redis  │  │  MinIO  │  │ Postgre │  │ RabbitMQ││
│  │  8.0    │  │  7.0    │  │ 对象存储│  │SQL+pgvec│  │  消息   ││
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘│
└─────────────────────────────────────────────────────────────────┘
```

---

## 核心模块

### 1. AI 智能体模块 (Agent Module)

**位置**: `backend/src/main/java/com/xingchen/backend/ai/`

```
ai/
├── orchestrator/          # 🎯 编排器（核心）
│   └── AgentOrchestrator  # 用户级智能体编排
├── intent/               # 🎯 意图分类
│   ├── IntentClassifierInterface
│   ├── RegexIntentClassifier   # 正则分类
│   ├── EmbeddingIntentClassifier # 向量分类
│   └── HybridIntentClassifier   # 混合分类
├── llm/                   # 🤖 LLM 提供商
│   ├── LLMProviderInterface
│   ├── OpenAIProvider
│   ├── ClaudeProvider
│   └── GLMProvider
├── memory/                # 🧠 记忆管理
│   ├── MemoryContext      # 记忆上下文
│   ├── MemoryManager     # 记忆管理器
│   └── UserMemoryManager # 用户级记忆
├── tool/                  # 🔧 工具集
│   ├── Tool              # 工具接口
│   ├── AIToolRegistry   # 工具注册表
│   ├── ArticleQueryTool # 文章查询
│   ├── ArticlePublishTool # 文章发布
│   ├── ArticleUpdateTool  # 文章更新
│   ├── ArticleDeleteTool  # 文章删除
│   ├── CategoryTool     # 分类管理
│   └── TagTool          # 标签管理
└── model/                # 📦 数据模型
    ├── AIRequest
    ├── AIResponse
    └── Intent
```

**核心职责**:
- 用户意图分析与分类
- 多模型路由与调用
- 记忆上下文管理
- 工具调用编排
- 响应生成与返回

### 2. 内容管理模块 (Content Module)

**位置**: `backend/src/main/java/com/xingchen/backend/`

```
├── controller/             # REST API
│   ├── ArticleController
│   ├── CategoryController
│   ├── TagController
│   └── UserController
├── service/              # 业务逻辑
│   └── impl/
│       ├── ArticleServiceImpl
│       ├── CategoryServiceImpl
│       └── TagServiceImpl
├── mapper/              # 数据访问
│   ├── ArticleMapper
│   ├── CategoryMapper
│   └── TagMapper
└── entity/              # 实体
    ├── Article
    ├── Category
    └── Tag
```

### 3. 数据分析模块 (Analytics Module)

**位置**: `backend/src/main/java/com/xingchen/backend/service/impl/AnalyticsServiceImpl.java`

**核心功能**:
- PV/UV/跳出率统计
- 用户行为路径分析
- 内容热度分析
- SEO 效果追踪

---

## 数据流设计

### AI 请求处理流程

```
1. 用户请求
   │
   ▼
2. 网关鉴权 (Nginx → JWT验证)
   │
   ▼
3. AgentController 接收
   │
   ▼
4. SecurityFilterChain 安全过滤
   │   - 输入校验
   │   - 敏感词检测
   │   - 频率限制
   │
   ▼
5. IntentClassifier.classify() 意图分类
   │   - Regex 快速匹配
   │   - Embedding 向量相似度
   │   - Hybrid 综合判断
   │
   ▼
6. AgentOrchestrator.handle() 编排执行
   │
   ├── 6.1 读取用户记忆 (UserMemoryManager)
   │
   ├── 6.2 构建 Prompt
   │
   ├── 6.3 调用 LLM (UserLLMProviderManager)
   │       - 熔断保护 (Resilience4j)
   │       - 多模型 fallback
   │
   ├── 6.4 解析 Tool Calls
   │
   ├── 6.5 执行工具 (AIToolRegistry)
   │       - ArticleQueryTool
   │       - RAG 查询
   │
   ├── 6.6 保存记忆 (UserMemoryManager)
   │
   ▼
7. 返回 AIResponse
   │
   ▼
8. 流式响应 (SSE) 或 普通响应
```

### 自动增长闭环流程

```
┌──────────────────────────────────────────────────────────────┐
│                     自动增长闭环                              │
└──────────────────────────────────────────────────────────────┘

[定时任务触发]
     │
     ▼
[Analytics 数据采集]
     │ - 搜索词统计
     │ - 页面停留
     │ - 跳出率
     │
     ▼
[分析 Agent - OpportunityDiscovery]
     │ - 识别内容缺口
     │ - 评估搜索需求
     │ - 生成写作任务
     │
     ▼
[写作 Agent - ContentGeneration]
     │ - 主题确定
     │ - 大纲生成
     │ - 内容创作
     │ - SEO 优化
     │
     ▼
[内容发布]
     │ - 草稿创建
     │ - 自动发布
     │
     ▼
[SEO 优化]
     │ - 关键词注入
     │ - 内链添加
     │ - 结构优化
     │
     ▼
[效果追踪]
     │ - 流量监控
     │ - 排名追踪
     │ - 转化分析
     │
     ▼
[反馈 → 新一轮优化]
```

---

## 技术选型

### 后端技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Spring Boot | 3.4.x | Web 框架 |
| ORM | MyBatis-Flex | 1.11.x | 灵活 ORM |
| 认证 | Sa-Token | 1.38.x | 轻量认证 |
| 缓存 | Redis | - | 会话/缓存 |
| 主数据库 | MySQL | 8.0 | 业务数据存储 |
| 向量数据库 | PostgreSQL + pgvector | 15+ | RAG 向量检索（主力） |
| 消息 | RabbitMQ | - | 异步消息/事件驱动 |
| AI | LangChain4j + 自研编排器 | 0.35.x | Agent 框架 |

### 前端技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 框架 | Vue 3.3 | Composition API |
| 构建 | Vite 4.x | 快速 HMR |
| 语言 | TypeScript | 类型安全 |
| UI | Element Plus | 组件库 |
| 状态 | Pinia | 轻量状态 |
| 样式 | Tailwind CSS | 原子化 CSS |

### AI 基础设施

| 组件 | 技术 | 用途 |
|------|------|------|
| 向量数据库 | PostgreSQL + pgvector（主力） | RAG 检索 |
| Embedding | BGE / OpenAI text-embedding-3 | 文本向量化 |
| LLM | Claude/GPT-4/GLM/Baidu/Ollama | 内容生成 |
| Agent | LangChain4j + 自研编排器 | 工作流编排 |
| 搜索增强 | Tavily API | 联网搜索 |

---

## 扩展性设计

### 1. 插件化 Tool 扩展

```java
// 新增工具只需实现 Tool 接口
@Component
public class NewTool implements Tool {
    @Override
    public String getName() {
        return "newTool";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        // 实现工具逻辑
        return Map.of("result", "success");
    }
}
// 自动注册到 AIToolRegistry
```

### 2. 多模型 Fallback

```java
public class UserLLMProviderManager {
    // 按优先级调用，失败时自动切换
    private List<LLMProviderInterface> providers = Arrays.asList(
        new ClaudeProvider(),    // Primary
        new OpenAIProvider(),     // Fallback 1
        new GLMProvider()         // Fallback 2
    );
}
```

### 3. 向量数据库解耦

```java
// 支持多种向量数据库，通过配置切换
@Configuration
public class VectorConfig {
    @Bean
    public VectorStore vectorStore(
            @Value("${vector.db:postgresql}") String dbType) {
        return switch (dbType) {
            case "qdrant" -> new QdrantVectorStore();
            case "milvus" -> new MilvusVectorStore();
            default -> new PostgreSQLVectorStore();  // pgvector
        };
    }
}
```

---

## 安全设计

### 1. 输入安全

```
SecurityFilterChain 流水线:
├── XSS 过滤
├── SQL 注入检测
├── 敏感词过滤
├── 请求频率限制
└── API Key 校验
```

### 2. 认证授权

```
Sa-Token 认证流程:
├── 登录 → 生成 Token
├── 请求 → Header 携带 Token
├── 网关 → Token 验证
└── 服务 → 权限校验
```

### 3. 数据安全

```
├── API Key 加密存储 (AES)
├── 数据库敏感字段加密
├── HTTPS 传输加密
└── 定期安全审计
```

---

## 📚 相关文档

- [README](../README.md) - 项目概览
- [DEPLOYMENT.md](DEPLOYMENT.md) - 部署指南
- [CONTRIBUTING.md](CONTRIBUTING.md) - 贡献指南
- [CHANGELOG.md](CHANGELOG.md) - 变更日志
