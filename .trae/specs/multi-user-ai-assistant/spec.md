# 多用户独立记忆智能助手系统规格说明书

## 一、需求背景

用户希望将博客系统升级为类似 OpenClaw 的智能助手系统，具备以下核心能力：

* **核心功能**：为不同用户自动撰写博客文章

* **多用户 API Key**：用户可自定义配置自己的 AI API Key（也可使用系统默认）

* **自主定时任务**：AI 可自主创建和管理定时任务

* **自然语言命令**：用户下达命令即可自动执行

* WebSocket 实时通信

* SQLite + Markdown 本地记忆系统

* 多用户独立记忆隔离

* 飞书平台集成

## 二、与现有系统的整合

### 2.1 现有 AI 能力

| 现有服务                 | 功能          | 整合方式       |
| -------------------- | ----------- | ---------- |
| AIService            | 对话、RAG、联网搜索 | 作为默认 AI 引擎 |
| KnowledgeBaseService | 知识库管理       | 用户记忆检索     |
| WebSearchService     | 联网搜索        | 文章素材获取     |
| 百度 API 配置            | AI 对话能力     | 作为系统默认     |
| GLM API 配置           | 备用 AI       | 作为备选       |

### 2.2 整合后的 AI 调用策略

```
用户发送消息
      │
      ▼
┌─────────────────┐
│ 检查用户 API Key │
└────────┬────────┘
         │
    ┌────┴────┐
    │ 有自定义 │────使用用户 API Key
    │   Key   │
    └────┬────┘
         │无
         ▼
┌─────────────────┐
│ 使用系统默认 API │────百度/GLM
└─────────────────┘
```

## 三、核心功能设计

### 3.1 用户 API Key 管理（最重要）

```java
// 用户自定义 API Key 配置
@Entity
public class UserApiKey {
    private Long id;
    private Long userId;              // 用户ID
    private String provider;          // AI提供商: OPENAI, ANTHROPIC, ZHIPU, BAIDU, CUSTOM
    private String apiKey;           // 用户自己的 API Key
    private String baseUrl;          // 自定义 API 地址
    private String defaultModel;     // 默认模型
    private Boolean enabled;         // 是否启用
    private Integer quota;           // 每月配额限制
    private Integer used;            // 已使用次数
    private LocalDateTime expireAt;  // 过期时间
}
```

**支持的主流 AI 提供商：**

| Provider     | 模型示例                    | 官方文档                | 与现有系统关系        |
| ------------ | ----------------------- | ------------------- | -------------- |
| 百度千帆         | qianfan-code-latest     | qianfan.baidu.com   | **现有** - 直接复用  |
| 智谱GLM        | glm-4-flash, glm-4-plus | zhipuai.cn          | **现有** - 配置已存在 |
| OpenAI       | gpt-4o, gpt-4o-mini     | openai.com          | 新增             |
| Anthropic    | claude-3-5-sonnet       | anthropic.com       | 新增             |
| Azure OpenAI | gpt-4o                  | azure.microsoft.com | 新增             |
| 自定义          | 任意兼容 API                | 用户自定义               | 新增             |

### 3.2 自然语言命令系统

```java
// 命令解析服务（使用现有 AIService）
@Service
public class CommandParserService {

    private final AIService aiService;  // 现有服务
    
    public Command parse(String userMessage) {
        // 使用现有 AI 服务解析用户意图
        String intent = aiService.classifyIntent(userMessage);
        
        return switch (intent) {
            case "写文章", "create_article" -> parseCreateArticleCommand(userMessage);
            case "创建定时任务", "schedule_task" -> parseScheduleTaskCommand(userMessage);
            case "查看任务", "list_tasks" -> parseListTasksCommand(userMessage);
            case "取消任务", "cancel_task" -> parseCancelTaskCommand(userMessage);
            case "发布文章", "publish_article" -> parsePublishCommand(userMessage);
            case "聊天", "chat" -> Command.chat(userMessage);
            default -> Command.chat(userMessage);
        };
    }
}
```

**命令示例：**

| 用户命令              | AI 解析结果                                            | 执行动作       |
| ----------------- | -------------------------------------------------- | ---------- |
| "帮我写一篇关于 AI 的文章"  | create\_article(topic="AI")                        | 生成文章草稿     |
| "每天早上8点帮我写一篇热点资讯" | schedule\_task(cron="0 0 8 \* \* ?", topic="热点资讯") | 创建定时任务     |
| "这周六早上9点发一篇周报"    | schedule\_task(cron="0 0 9 \* \* 6", topic="周报")   | 创建定时任务     |
| "取消每天早上8点的任务"     | cancel\_task(taskId=xxx)                           | 删除定时任务     |
| "帮我看看有哪些定时任务"     | list\_tasks()                                      | 列出所有任务     |
| "把这篇文章发了"         | publish\_article(articleId=xxx)                    | 发布文章       |
| "今天天气怎么样"         | chat()                                             | 调用现有 AI 对话 |

### 3.3 智能定时任务系统

```java
// 定时任务实体
@Entity
public class ScheduledTask {
    private Long id;
    private Long userId;
    private String taskName;         // 任务名称
    private String cronExpression;   // Cron 表达式
    private String topicTemplate;    // 文章主题模板
    private GenerationStrategy strategy; // 生成策略
    private Integer wordCount;       // 字数要求
    private String style;           // 写作风格
    private Long categoryId;        // 分类
    private List<String> keywords; // 关键词
    
    private TaskStatus status;      // 状态: ACTIVE, PAUSED, COMPLETED
    private LocalDateTime lastRun;  // 上次执行时间
    private LocalDateTime nextRun;  // 下次执行时间
    private Integer runCount;       // 执行次数
    
    private Boolean autoPublish;    // 是否自动发布
    private Boolean notifyFeishu;   // 是否飞书通知
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 3.4 文章自动生成流程（整合现有服务）

```
用户命令: "帮我写一篇关于 AI 发展的文章"
                    │
                    ▼
        ┌───────────────────────┐
        │   命令解析 (现有AI)    │
        └───────────┬───────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │   检查用户 API Key    │
        │  - 有 Key → 使用用户 Key│
        │  - 无 Key → 使用百度API │
        └───────────┬───────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │   获取用户记忆         │
        │  - 写作风格偏好        │
        │  - 常用分类/标签       │
        └───────────┬───────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │   联网搜索素材         │ ← 复用现有 WebSearchService
        └───────────┬───────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │   AI 生成文章          │ ← 复用现有 AIService
        └───────────┬───────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │   知识库索引           │ ← 复用现有 KnowledgeBaseService
        └───────────┬───────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │   保存为草稿           │
        └───────────┬───────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │   通知用户             │
        │  - 飞书卡片消息        │
        └───────────────────────┘
```

## 四、架构设计

### 4.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        多用户智能助手系统架构                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐              │
│   │   飞书平台   │◄───►│   WebSocket  │◄───►│  命令解析器   │              │
│   │  (消息推送)  │     │   实时通信   │     │  (AI Intent) │              │
│   └──────────────┘     └──────────────┘     └──────┬───────┘              │
│                                                    │                       │
│   ┌──────────────┐     ┌──────────────┐     ┌──────▼───────┐              │
│   │   前端页面   │◄───►│   控制器     │◄───►│  任务调度器   │              │
│   │  (聊天界面)  │     │   REST API   │     │  Scheduler   │              │
│   └──────────────┘     └──────────────┘     └──────┬───────┘              │
│                                                    │                       │
│   ┌─────────────────────────────────────────────────┼───────────────────────┐
│   │                                    记忆引擎      │                       │
│   │   ┌─────────────┐    ┌─────────────┐    ┌─────▼─────┐                │
│   │   │ 用户API Key │    │  SQLite     │    │ Markdown  │                │
│   │   │  配置管理   │    │ (对话历史)  │    │ (长期记忆) │                │
│   │   └─────────────┘    └─────────────┘    └───────────┘                │
│   └───────────────────────────────────────────────────────────────────────┘
│                                                                             │
│   ════════════════════════════════════════════════════════════════════════│
│                           现有系统复用                                      │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│   │  AIService   │  │KnowledgeBase │  │WebSearchSvc  │  │ 百度/GLM API│ │
│   │  (对话/RAG)  │  │   Service    │  │  (联网搜索)  │  │  (现有配置) │ │
│   └──────────────┘  └──────────────┘  └──────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 三级记忆系统

| 层级   | 存储方式     | 容量     | 用途        | 生命周期 |
| ---- | -------- | ------ | --------- | ---- |
| 工作内存 | Redis    | \~10KB | 当前对话上下文   | 会话内  |
| 短期记忆 | SQLite   | \~100条 | 对话历史、实体   | 30天  |
| 长期记忆 | Markdown | 无限     | 用户偏好、重要事实 | 永久   |

### 4.3 数据隔离策略

* 每个用户独立的 SQLite 表：conversation\_{userId}、entity\_{userId}、preference\_{userId}

* 每个用户独立的 Markdown 文件目录：\~/.myblog/agent/users/{userId}/

* 每个用户独立的 API Key 配置

## 五、数据库设计

### 5.1 MySQL 表

```sql
-- 用户 API Key 配置表（核心）
CREATE TABLE `user_api_key` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `provider` VARCHAR(20) NOT NULL DEFAULT 'BAIDU' COMMENT 'AI提供商',
    `api_key` VARCHAR(500) COMMENT 'API Key',
    `base_url` VARCHAR(500) COMMENT '自定义API地址',
    `default_model` VARCHAR(100) COMMENT '默认模型',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `quota` INT COMMENT '每月配额',
    `used` INT DEFAULT 0 COMMENT '已使用次数',
    `expire_at` DATETIME COMMENT '过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- AI 助手配置表
CREATE TABLE `ai_assistant` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `name` VARCHAR(50) DEFAULT '智能助手' COMMENT '助手名称',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `personality` VARCHAR(500) COMMENT '人设描述',
    `system_prompt` TEXT COMMENT '系统提示词',
    `default_greeting` VARCHAR(500) DEFAULT '你好！我是你的AI写作助手' COMMENT '默认开场白',
    `default_style` VARCHAR(100) DEFAULT '专业但通俗易懂' COMMENT '默认写作风格',
    `max_memory_rounds` INT DEFAULT 50 COMMENT '最大记忆轮次',
    `evolution_interval` INT DEFAULT 10 COMMENT '进化间隔',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 定时任务表
CREATE TABLE `scheduled_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `cron_expression` VARCHAR(50) NOT NULL COMMENT 'Cron表达式',
    `topic_template` VARCHAR(500) COMMENT '文章主题模板',
    `strategy` VARCHAR(20) DEFAULT 'HOT_TOPIC' COMMENT '生成策略',
    `word_count` INT DEFAULT 2000 COMMENT '字数要求',
    `style` VARCHAR(100) COMMENT '写作风格',
    `category_id` BIGINT COMMENT '分类ID',
    `keywords` VARCHAR(500) COMMENT '关键词',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    `last_run_time` DATETIME COMMENT '上次执行时间',
    `next_run_time` DATETIME COMMENT '下次执行时间',
    `run_count` INT DEFAULT 0 COMMENT '执行次数',
    `auto_publish` TINYINT DEFAULT 0 COMMENT '是否自动发布',
    `notify_feishu` TINYINT DEFAULT 1 COMMENT '是否飞书通知',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 文章生成日志表
CREATE TABLE `article_generation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `topic` VARCHAR(500) COMMENT '生成主题',
    `task_id` BIGINT COMMENT '来源任务ID',
    `strategy` VARCHAR(20) NOT NULL COMMENT '生成策略',
    `status` VARCHAR(20) NOT NULL COMMENT '状态',
    `article_id` BIGINT COMMENT '关联文章ID',
    `word_count` INT COMMENT '实际字数',
    `error_message` VARCHAR(1000),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 飞书配置表
CREATE TABLE `feishu_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `webhook_url` VARCHAR(500) COMMENT 'Webhook地址',
    `enabled` TINYINT DEFAULT 0 COMMENT '是否启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 5.2 SQLite 表

```sql
CREATE TABLE IF NOT EXISTS `conversation` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `role` TEXT NOT NULL,
    `content` TEXT NOT NULL,
    `tokens` INTEGER,
    `metadata` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `entity` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` TEXT NOT NULL,
    `type` TEXT NOT NULL,
    `content` TEXT NOT NULL,
    `importance` INTEGER DEFAULT 0,
    `last_mentioned` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `preference` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `key` TEXT NOT NULL UNIQUE,
    `value` TEXT NOT NULL,
    `confidence` REAL DEFAULT 1.0,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## 六、API 设计

### 6.1 API Key 管理（新增）

| 方法  | 路径                     | 说明             |
| --- | ---------------------- | -------------- |
| GET | /api/user/apikey       | 获取当前用户 API Key |
| PUT | /api/user/apikey       | 更新 API Key     |
| GET | /api/user/apikey/usage | 查看使用统计         |

### 6.2 助手配置

| 方法  | 路径                    | 说明     |
| --- | --------------------- | ------ |
| GET | /api/assistant/config | 获取助手配置 |
| PUT | /api/assistant/config | 更新助手配置 |

### 6.3 定时任务

| 方法     | 路径                 | 说明       |
| ------ | ------------------ | -------- |
| GET    | /api/task          | 获取所有定时任务 |
| POST   | /api/task          | 创建定时任务   |
| PUT    | /api/task/{id}     | 更新任务     |
| DELETE | /api/task/{id}     | 删除任务     |
| POST   | /api/task/{id}/run | 立即执行     |

### 6.4 聊天/命令

| 方法   | 路径                     | 说明            |
| ---- | ---------------------- | ------------- |
| GET  | /ws/assistant/{userId} | WebSocket 连接  |
| POST | /api/assistant/chat    | HTTP 聊天（复用现有） |

### 6.5 飞书

| 方法       | 路径                  | 说明     |
| -------- | ------------------- | ------ |
| POST     | /api/feishu/webhook | 飞书事件回调 |
| GET/POST | /api/feishu/config  | 飞书配置   |

## 七、配置文件

```yaml
# application.yaml 新增/修改

# 现有 AI 配置（保持不变）
glm:
  api:
    key: ${GLM_API_KEY:52e42b5a7a034325828e148b588f90e3.JJR9IxhUrzpDQgmn}
    base-url: ${GLM_BASE_URL:https://open.bigmodel.cn/api/paas/v4}
    model: ${GLM_MODEL:glm-4-flash}

baidu:
  api:
    key: ${BAIDU_API_KEY:bce-v3/ALTAKSP-W8fnSI7P0cqEcBxE6SYuM/94fd0c1e5615d09b244576df2d68dfd0739ed5f2}
    base-url: ${BAIDU_BASE_URL:https://qianfan.baidubce.com/v2/coding}
    model: ${BAIDU_MODEL:qianfan-code-latest}

# 新增：智能助手配置
agent:
  workspace: ${AGENT_WORKSPACE:~/.myblog/agent}
  sqlite:
    path: ${AGENT_SQLITE_PATH:~/.myblog/agent/memory.db}
  memory:
    max-rounds: 50
    evolution-interval: 10
    retention-days: 30
  default-provider: BAIDU  # 默认使用百度

# 新增：支持的用户 API 提供商
ai-providers:
  openai:
    base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
    models: [gpt-4o, gpt-4o-mini, gpt-4-turbo]
  anthropic:
    base-url: ${ANTHROPIC_BASE_URL:https://api.anthropic.com}
    models: [claude-3-5-sonnet, claude-3-opus]
  zhipu:
    base-url: ${ZHIPU_BASE_URL:https://open.bigmodel.cn/api/paas/v4}
    models: [glm-4-flash, glm-4-plus, glm-4]
  baidu:
    base-url: ${BAIDU_BASE_URL:https://qianfan.baidubce.com/v2}
    models: [qianfan-code-latest, ernie-bot-4]
  azure:
    base-url: ${AZURE_BASE_URL:}
    models: [gpt-4o, gpt-35-turbo]

feishu:
  enabled: ${FEISHU_ENABLED:false}
  app:
    id: ${FEISHU_APP_ID:}
    secret: ${FEISHU_APP_SECRET:}
```

## 八、技术选型

| 组件          | 技术                   | 版本                | 复用说明     |
| ----------- | -------------------- | ----------------- | -------- |
| WebSocket   | Spring WebSocket     | Spring Boot 3.5.x | 新增       |
| SQLite      | SQLite JDBC          | 3.42.x            | 新增       |
| Markdown    | CommonMark           | 0.22.x            | 现有       |
| 任务调度        | Spring @Scheduled    | 内置                | 复用       |
| AI 对话       | AIService            | 现有                | **直接复用** |
| 知识库         | KnowledgeBaseService | 现有                | **直接复用** |
| 联网搜索        | WebSearchService     | 现有                | **直接复用** |
| 飞书 SDK      | Open Feishu SDK      | 7.4.0             | 新增       |
| HTTP Client | OkHttp               | 4.12.0            | 新增       |

## 九、实施计划

| 阶段       | 任务                     | 复用/新增  | 预计工作量 |
| -------- | ---------------------- | ------ | ----- |
| Phase 1  | 基础架构（数据库、配置、WebSocket） | 新增     | 0.5天  |
| Phase 2  | 实体与 Mapper             | 新增     | 0.5天  |
| Phase 3  | 用户 API Key 管理          | 新增     | 1天    |
| Phase 4  | 命令解析系统                 | 新增     | 0.5天  |
| Phase 5  | 定时任务系统                 | 新增     | 1天    |
| Phase 6  | 文章自动生成（集成现有AI）         | **复用** | 0.5天  |
| Phase 7  | 记忆引擎                   | 新增     | 1天    |
| Phase 8  | 飞书集成                   | 新增     | 0.5天  |
| Phase 9  | 前端界面                   | 新增     | 1天    |
| Phase 10 | 测试优化                   | -      | 1天    |

## 十、验收标准

1. ✅ 用户可自定义配置自己的 API Key
2. ✅ 支持多种主流 AI 提供商（百度、GLM 为默认）
3. ✅ 自然语言命令自动解析执行
4. ✅ 定时任务自动生成文章（使用现有 AI 服务）
5. ✅ 用户记忆完全隔离
6. ✅ 飞书消息推送
7. ✅ 自主进化学习用户偏好
8. ✅ 前端可视化配置
9. ✅ 现有 AI 服务（对话/RAG/搜索）完全复用

