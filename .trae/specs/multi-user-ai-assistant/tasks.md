# 多用户独立记忆智能助手系统 - 任务清单

## Phase 1: 基础架构搭建

- [x] 1.1 创建 MySQL 数据表（user_api_key, ai_assistant, scheduled_task, feishu_config）
- [x] 1.2 配置 pom.xml 添加 SQLite、OkHttp、WebSocket 依赖
- [x] 1.3 创建 WebSocket 配置类
- [ ] 1.4 添加 application.yaml 配置项

## Phase 2: 实体与 Mapper

- [x] 2.1 创建 UserApiKey 实体类
- [x] 2.2 创建 AIAssistant 实体类
- [x] 2.3 创建 ScheduledTask 实体类
- [x] 2.4 创建 FeishuConfig 实体类
- [x] 2.5 创建对应的 Mapper 接口

## Phase 3: 用户 API Key 管理

- [x] 3.1 实现 UserApiKeyService（CRUD，配额管理）
- [ ] 3.2 实现多 AI 提供商适配器（OpenAI, Anthropic, 智谱, 百度）
- [x] 3.3 实现动态 API Key 选择逻辑

## Phase 4: 命令解析系统

- [x] 4.1 创建 Command 实体类
- [x] 4.2 实现 CommandParserService（意图分类）
- [x] 4.3 实现文章生成命令解析
- [x] 4.4 实现定时任务命令解析
- [x] 4.5 实现文章发布命令解析

## Phase 5: 定时任务系统

- [x] 5.1 实现 ScheduledTaskService（CRUD、状态管理）
- [ ] 5.2 实现动态任务调度器
- [x] 5.3 创建 ArticleGenerationTask（文章生成定时任务）
- [x] 5.4 实现 Cron 表达式解析（自然语言转 Cron）

## Phase 6: 文章自动生成

- [x] 6.1 实现 ArticleGenerationService
- [x] 6.2 实现文章生成流程（大纲、正文、SEO）
- [x] 6.3 集成用户 API Key
- [x] 6.4 实现文章生成日志记录

## Phase 7: 记忆引擎

- [ ] 7.1 实现 SQLite 数据库初始化服务
- [ ] 7.2 实现 Markdown 文件管理服务
- [ ] 7.3 实现 MemoryService（记忆存储与检索）
- [ ] 7.4 实现 EvolutionEngine（自主进化引擎）
- [ ] 7.5 实现 UserContextService（上下文构建）

## Phase 8: WebSocket 通信

- [x] 8.1 创建 AssistantWebSocketEndpoint
- [x] 8.2 实现消息处理与命令执行
- [ ] 8.3 创建 FeishuWebhookController
- [ ] 8.4 实现飞书消息推送

## Phase 9: REST API

- [x] 9.1 创建 UserApiKeyController
- [x] 9.2 创建 AssistantController
- [x] 9.3 创建 TaskController
- [ ] 9.4 创建 FeishuController

## Phase 10: 前端集成

- [ ] 10.1 创建 API Key 设置页面
- [ ] 10.2 创建助手设置页面
- [ ] 10.3 创建定时任务管理页面
- [ ] 10.4 创建聊天组件

## Phase 11: 测试验证

- [ ] 11.1 单元测试
- [ ] 11.2 集成测试
- [ ] 11.3 功能验证

---

# 任务依赖关系

```
Phase 1 (基础架构)
    │
    ├── 1.1 创建数据表 ──────────┐
    ├── 1.2 添加依赖   ──────────┤
    ├── 1.3 WebSocket配置 ───────┤
    └── 1.4 应用配置 ────────────┤
                                  │
Phase 2 (实体与Mapper) ◄─────────┘
    │
    ├── 2.1-2.4 实体类 ──────────┤
    └── 2.5 Mapper接口 ────────────┤
                                  │
Phase 3 (API Key管理) ◄───────────┘
    │
    ├── 3.1 UserApiKeyService ────┤
    ├── 3.2 AI提供商适配器 ────────┤
    └── 3.3 动态Key选择 ──────────┤
                                  │
Phase 4 (命令解析) ◄──────────────┘
    │
    ├── 4.1-4.2 命令解析基础 ─────┤
    ├── 4.3 文章命令 ─────────────┤
    ├── 4.4 定时任务命令 ─────────┤
    └── 4.5 发布命令 ─────────────┤
                                  │
Phase 5 (定时任务) ◄──────────────┘
    │
    ├── 5.1 任务服务 ─────────────┤
    ├── 5.2 动态调度器 ───────────┤
    ├── 5.3 生成任务 ─────────────┤
    └── 5.4 Cron解析 ─────────────┤
                                  │
Phase 6 (文章生成) ◄──────────────┘
    │
    ├── 6.1 生成服务 ─────────────┤
    ├── 6.2 生成流程 ─────────────┤
    ├── 6.3 集成API Key ──────────┤
    └── 6.4 日志记录 ─────────────┤
                                  │
Phase 7 (记忆引擎) ◄──────────────┘
    │
    ├── 7.1 SQLite初始化 ─────────┤
    ├── 7.2 Markdown管理 ─────────┤
    ├── 7.3 MemoryService ────────┤
    ├── 7.4 EvolutionEngine ──────┤
    └── 7.5 UserContextService ───┘
              │
Phase 8 (WebSocket) ◄─────────────┘
    │
    ├── 8.1 WebSocket端点 ────────┤
    ├── 8.2 消息处理 ─────────────┤
    ├── 8.3 飞书控制器 ───────────┤
    └── 8.4 飞书推送 ─────────────┘
              │
Phase 9 (REST API) ◄──────────────┘
    │
    ├── 9.1 UserApiKeyController ─┤
    ├── 9.2 AssistantController ──┤
    ├── 9.3 TaskController ───────┤
    └── 9.4 FeishuController ─────┘
              │
Phase 10 (前端) ◄─────────────────┘
              │
Phase 11 (测试) ◄─────────────────┘
```
