# BlogGrowth AI 全方面测试规划

> 生成日期: 2026-06-10
> 状态: 待执行

---

## 一、现状概览

### 已有测试

| 类别 | 数量 | 覆盖范围 |
|------|------|----------|
| 后端单元测试 | 19 个 | Service 层基础 CRUD 操作 |
| 前端单元测试 | 10 个 | 少数组件 + store + API |
| 前端 E2E 测试 | 3 个 | 登录、认证、文章浏览 |

### 测试框架

| 框架 | 后端 | 前端 |
|------|------|------|
| 单元测试 | JUnit 5 + Mockito | Vitest + Vue Test Utils |
| E2E 测试 | - | Playwright |
| 覆盖率 | - | v8 |

---

## 二、后端测试缺口与计划

### Phase 1: Controller 层测试（高优先级）

> 目标：确保所有 HTTP 入口有基本的请求/响应测试

| # | Controller | 需要测试的场景 | 优先级 |
|---|-----------|---------------|--------|
| 1 | `AuthController` | 登录成功/失败、MFA 流程、验证码、Token 刷新、退出登录 | P0 |
| 2 | `UserController` | 注册校验、密码更新、个人信息 CRUD、头像上传 | P0 |
| 3 | `ArticleController` | 文章 CRUD 权限校验、分页查询、相关文章、点赞/收藏 | P0 |
| 4 | `CommentController` | 评论 CRUD、嵌套回复、评论点赞、权限校验 | P0 |
| 5 | `CategoryController` | 分类 CRUD、权限校验 | P1 |
| 6 | `TagController` | 标签 CRUD、热门标签 | P1 |
| 7 | `PasswordController` | 密码重置流程、验证码校验 | P0 |
| 8 | `MfaController` | MFA 启用/禁用/验证全流程 | P0 |
| 9 | `VerificationController` | 验证码发送/校验 | P1 |
| 10 | `FileController` | 文件上传、下载、权限校验 | P1 |
| 11 | `AgentController` | AI 对话、会话管理、工具调用 | P1 |
| 12 | `AIController` / `AIV2Controller` | 流式响应、多 LLM 切换 | P1 |

**测试类型**: `@WebMvcTest` + MockMvc（Spring Boot 集成测试）
**建议文件路径**: `backend/src/test/java/com/xingchen/backend/controller/`

### Phase 2: Agent 系统测试（高优先级）

> 目标：AI 智能体核心架构需要独立测试，这是项目的核心竞争力

| # | 组件 | 测试内容 | 优先级 |
|---|------|---------|--------|
| 1 | `AgentOrchestrator` | 消息重要性评估、意图分类路由、工具选择、错误降级 | P0 |
| 2 | `BaseAgent` | 生命周期回调、超时处理、重试机制 | P0 |
| 3 | `ContentGenerationAgent` | 内容生成、Prompt 构建、LLM 失败回退 | P0 |
| 4 | `ContentAuditAgent` | 内容审核规则、敏感词过滤 | P0 |
| 5 | `ReActAgent` | 思考-行动循环、工具调用、最大步数限制 | P0 |
| 6 | `ReActAgentWithFunctionCalling` | 函数调用解析、参数提取 | P0 |
| 7 | `UnifiedMemoryServiceImpl` | 记忆存储/检索、重要性评分、遗忘机制 | P0 |
| 8 | `IntentClassifier` | 意图分类准确性、边界 case | P0 |
| 9 | `AIToolRegistry` | 工具注册/查找、权限检查 | P1 |
| 10 | `SecurityFilterChain` | Prompt 注入检测、输入过滤、输出过滤 | P0 |

**测试类型**: 单元测试（Mock LLM 响应）+ 集成测试（使用 TestLLM）
**建议文件路径**: `backend/src/test/java/com/xingchen/backend/agent/`

### Phase 3: Service 层增强测试

> 现有 19 个 Service 测试需要补充更多边界条件

| # | Service | 需要补充的测试 | 优先级 |
|---|---------|--------------|--------|
| 1 | `ArticleServiceImpl` | 批量查询场景、N+1 修复验证、缓存一致性、并发写入 | P0 |
| 2 | `UserServiceImpl` | 密码强度校验、并发注册、重复用户名处理 | P0 |
| 3 | `CommentServiceImpl` | 嵌套评论树、评论分页、并发点赞 | P1 |
| 4 | `FileServiceImpl` | 文件类型校验、大小限制、存储异常 | P1 |
| 5 | `AnalyticsServiceImpl` | 数据统计准确性、时间范围查询 | P2 |

### Phase 4: 基础设施组件测试

| # | 组件 | 测试内容 | 优先级 |
|---|------|---------|--------|
| 1 | `WebSocketConfig` / `WebSocketAuthInterceptor` | WebSocket 认证、权限拦截 | P1 |
| 2 | `SaTokenStpInterfaceImpl` | 角色权限解析 | P1 |
| 3 | `RedisLockUtil` | 分布式锁获取/释放、超时、重入 | P0 |
| 4 | `AesUtil` | 加密/解密、Key 管理 | P1 |
| 5 | `UserLLMProviderManager` | 多 Provider 切换、API Key 管理 | P1 |
| 6 | `GrowthOrchestrator` | 增长任务调度、定时任务触发 | P1 |
| 7 | `WorkflowEngine` | 工作流定义、任务节点执行、错误恢复 | P1 |
| 8 | `MessageRouter` | 消息路由、协议选择 | P1 |
| 9 | `PluginManager` | 插件加载/卸载、生命周期 | P2 |

### Phase 5: 集成测试（Integration Tests）

| # | 场景 | 测试内容 | 优先级 |
|---|------|---------|--------|
| 1 | 完整用户注册流程 | 注册 → 验证码 → 登录 → 个人信息 | P0 |
| 2 | 完整文章发布流程 | 创建文章 → 审核 → 发布 → 评论 → 点赞 | P0 |
| 3 | AI 对话完整流程 | 用户提问 → 意图识别 → 工具调用 → 生成回复 | P0 |
| 4 | 文件上传到展示流程 | 上传 → MinIO 存储 → URL 返回 → 前端展示 | P1 |
| 5 | 缓存一致性 | Redis 缓存命中/失效/更新 → 数据库一致性 | P0 |

**测试类型**: `@SpringBootTest` + Testcontainers（MySQL + Redis + RabbitMQ）
**建议文件路径**: `backend/src/test/java/com/xingchen/backend/integration/`

### Phase 6: 性能测试

| # | 场景 | 指标 | 优先级 |
|---|------|------|--------|
| 1 | 文章列表分页查询 | QPS > 100, P99 < 50ms | P1 |
| 2 | 批量标签查询（N+1 修复验证） | 对比修复前后的 SQL 数量 | P0 |
| 3 | 并发点赞/收藏 | 无脏数据，计数准确 | P0 |
| 4 | AI 响应超时 | 流式响应超时后的降级行为 | P0 |
| 5 | 缓存穿透/击穿 | Redis 不可用时的降级行为 | P1 |

---

## 三、前端测试缺口与计划

### Phase 1: 缺失的组件单元测试

> 现有 10 个组件测试，约 50+ 组件缺少测试

#### P0 - 核心业务组件

| # | 组件 | 需要测试的场景 |
|---|------|---------------|
| 1 | `LoginForm.vue` | 登录成功/失败、验证码校验、MFA 跳转、键盘快捷键 |
| 2 | `RegisterForm.vue` | 注册校验、验证码倒计时、密码强度提示、步骤导航 |
| 3 | `CommentInput.vue` | 输入校验、表情插入、提交/取消、字数限制 |
| 4 | `CommentList.vue` | 评论加载、点赞、删除、加载更多、回复加载 |
| 5 | `ArticleHeader.vue` | 作者信息展示、操作按钮、响应式布局 |
| 6 | `ArticleToc.vue` | 目录渲染、活跃项高亮、滚动同步、点击导航 |
| 7 | `ArticleComments.vue` | 评论列表 + 输入框整合、刷新 |
| 8 | `MarkdownRenderer.vue` | Markdown 渲染、代码高亮、数学公式、XSS 防护 |

#### P1 - UI 组件

| # | 组件 | 需要测试的场景 |
|---|------|---------------|
| 9 | `Pagination.vue` | 页码计算、跳转、每页大小切换、边界条件 |
| 10 | `SkeletonLoader.vue` | 各种类型渲染、紧凑模式 |
| 11 | `ErrorState.vue` | 错误类型展示、重试按钮、返回主页 |
| 12 | `EmptyState.vue` | 各种图标和状态展示 |
| 13 | `SearchBar.vue` | 搜索建议、历史记录、键盘导航 |
| 14 | `UserMenu.vue` | 用户信息展示、菜单项、登出 |

#### P2 - 技术组件

| # | 组件 | 需要测试的场景 |
|---|------|---------------|
| 15 | `TechBadge.vue` | 各种变体和尺寸渲染 |
| 16 | `TechCard.vue` | 玻璃效果、发光效果、悬停效果 |
| 17 | `FormInput.vue` | 输入校验、实时验证、失焦验证 |
| 18 | `MDEditor.vue` | 内容双向绑定、事件触发 |

#### P3 - Agent 相关组件

| # | 组件 | 需要测试的场景 |
|---|------|---------------|
| 19 | `AgentFloatPanel.vue` | 面板展开/收起、消息渲染 |
| 20 | `AgentChat.vue` | 消息列表、输入、流式渲染 |
| 21 | `AgentMessage.vue` | 消息渲染、工具调用展示 |
| 22 | `AgentThoughtProcess.vue` | 思考步骤展示、展开/折叠 |
| 23 | `AgentToolCalls.vue` | 工具调用列表、状态展示 |
| 24 | `AgentInputArea.vue` | 输入框、发送按钮、附件上传 |

### Phase 2: Composables 单元测试

| # | Composable | 测试内容 | 优先级 |
|---|-----------|---------|--------|
| 1 | `useArticleDetail` | 文章加载、错误处理、TOC 生成 | P0 |
| 2 | `useAsyncData` | 加载状态、错误状态、数据获取 | P0 |
| 3 | `useValidation` | 各种校验规则、自定义规则 | P0 |
| 4 | `useAvatar` | URL 处理、错误回退 | P1 |
| 5 | `useScrollSync` | 滚动同步逻辑 | P1 |
| 6 | `useScrollAnimation` | 动画触发条件 | P2 |
| 7 | `useTimeout` | 超时触发和清理 | P2 |
| 8 | `useCrudTable` | CRUD 操作封装 | P1 |
| 9 | `api-error` | 错误码映射、消息格式化 | P0 |
| 10 | `auth` | 认证状态检查、Token 管理 | P0 |

### Phase 3: 工具函数单元测试

| # | 工具文件 | 测试内容 | 优先级 |
|---|---------|---------|--------|
| 1 | `xss.js` | XSS 过滤规则、白名单 | P0 |
| 2 | `format.ts` | 日期格式化、数字格式化 | P1 |
| 3 | `transform.ts` | 数据转换函数 | P1 |
| 4 | `logger.js` | 日志级别、格式化 | P2 |
| 5 | `error-handler.js` | 错误分类、用户友好提示 | P1 |
| 6 | `notification.js` | 通知展示、自动关闭 | P2 |
| 7 | `loading.js` | 加载状态管理 | P2 |
| 8 | `util/index.js` | 通用工具函数 | P1 |

### Phase 4: Store 测试补充

| # | Store | 需要补充的测试 | 优先级 |
|---|-------|--------------|--------|
| 1 | `auth` | 登录/登出状态、Token 持久化、状态恢复 | P0 |
| 2 | `ui` | UI 状态管理、主题切换 | P1 |
| 3 | `其他 store` | 根据实际业务补充 | P2 |

### Phase 5: E2E 测试扩展

> 现有 3 个 E2E 测试，需要覆盖完整用户旅程

| # | 场景 | 测试步骤 | 优先级 |
|---|------|---------|--------|
| 1 | **用户注册登录全流程** | 注册 → 邮箱验证 → 登录 → 密码修改 → 退出 | P0 |
| 2 | **MFA 全流程** | 启用 MFA → 验证 → 禁用 → 备用码 | P0 |
| 3 | **文章完整流程** | 创建文章 → 编辑 → 发布 → 查看 → 评论 → 点赞 → 收藏 | P0 |
| 4 | **评论系统** | 发表评论 → 回复 → 点赞评论 → 删除评论 | P1 |
| 5 | **AI 对话** | 打开悬浮面板 → 发送消息 → 查看思考过程 → 查看工具调用 | P0 |
| 6 | **搜索功能** | 关键词搜索 → 查看结果 → 筛选 | P1 |
| 7 | **分类/标签浏览** | 分类列表 → 分类文章 → 标签云 → 标签文章 | P1 |
| 8 | **个人中心** | 个人信息编辑 → 头像上传 → 文章管理 → 收藏管理 | P1 |
| 9 | **后台管理** | 登录后台 → 仪表盘 → 文章管理 → 用户管理 | P1 |
| 10 | **响应式布局** | 移动端/平板/桌面三种视口测试 | P2 |
| 11 | **无障碍测试** | 键盘导航、屏幕阅读器兼容 | P2 |
| 12 | **错误恢复** | 网络断开 → 自动重试 → 数据恢复 | P1 |

---

## 四、测试基础设施增强

### 4.1 后端测试基础设施

```
backend/src/test/
├── java/com/xingchen/backend/
│   ├── controller/          # ← 新建：Controller 集成测试
│   │   ├── AuthControllerTest.java
│   │   ├── ArticleControllerTest.java
│   │   └── ...
│   ├── agent/               # ← 新建：Agent 系统测试
│   │   ├── AgentOrchestratorTest.java
│   │   ├── ContentGenerationAgentTest.java
│   │   └── ...
│   ├── integration/         # ← 新建：集成测试
│   │   ├── UserFlowIntegrationTest.java
│   │   └── ArticleFlowIntegrationTest.java
│   └── util/                # ← 新建：工具类测试
│       ├── RedisLockUtilTest.java
│       └── AesUtilTest.java
└── resources/
    └── application-test.yml # ← 新建：测试配置
```

**建议引入 Testcontainers** 用于 MySQL + Redis + RabbitMQ 的集成测试：
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

### 4.2 前端测试基础设施

```
front/tests/
├── unit/
│   ├── components/          # ← 需要补充 40+ 组件测试
│   │   ├── LoginForm.test.js
│   │   ├── CommentInput.test.js
│   │   └── ...
│   ├── composables/         # ← 新建：Composables 测试
│   │   ├── useArticleDetail.test.ts
│   │   └── useValidation.test.ts
│   ├── utils/               # ← 新建：工具函数测试
│   │   ├── xss.test.js
│   │   └── format.test.ts
│   └── stores/              # ← 需要补充
│       └── auth.test.js
├── e2e/
│   ├── auth.spec.js         # 已有
│   ├── articles.spec.js     # 已有
│   ├── login-flow.spec.js   # 已有
│   ├── comment-system.spec.js  # ← 新建
│   ├── ai-chat.spec.js         # ← 新建
│   └── user-center.spec.js     # ← 新建
└── fixtures/                # ← 新建：测试数据 fixtures
    ├── articles.json
    └── users.json
```

**建议引入 MSW (Mock Service Worker)** 用于前端 API Mock：
- 项目已有 `msw.workerDirectory` 配置，可直接使用

---

## 五、测试执行策略

### 5.1 CI/CD 流水线集成

```
┌─────────────────────────────────────────────────────┐
│                   Git Push / PR                      │
├─────────────────────────────────────────────────────┤
│  Stage 1: 快速反馈 (< 2 min)                        │
│  ├── 后端: mvn test (仅单元测试)                     │
│  └── 前端: npm run test:unit (Vitest)               │
├─────────────────────────────────────────────────────┤
│  Stage 2: 集成测试 (< 10 min)                        │
│  ├── 后端: @SpringBootTest (Testcontainers)          │
│  └── 前端: Playwright E2E (关键流程)                 │
├─────────────────────────────────────────────────────┤
│  Stage 3: 全面测试 (< 30 min) [Nightly]             │
│  ├── 后端: 全量测试 + 覆盖率报告                      │
│  ├── 前端: 全量 E2E + 覆盖率报告                      │
│  └── 性能测试: JMeter/Gatling                        │
└─────────────────────────────────────────────────────┘
```

### 5.2 覆盖率目标

| 层级 | 当前估算 | 目标 |
|------|---------|------|
| 后端 Service 行覆盖率 | ~40% | > 80% |
| 后端 Controller 行覆盖率 | ~0% | > 70% |
| 后端 Agent 行覆盖率 | ~0% | > 75% |
| 前端组件行覆盖率 | ~15% | > 70% |
| 前端 Composables 行覆盖率 | ~0% | > 80% |
| 前端工具函数行覆盖率 | ~10% | > 90% |
| 前端 E2E 关键路径覆盖率 | ~10% | > 60% |

---

## 六、执行顺序与里程碑

### Sprint 1 (第 1-2 周): 核心覆盖

- [ ] 后端: Controller 层 P0 测试（Auth, Article, Comment）
- [ ] 后端: Agent 系统 P0 测试（AgentOrchestrator, BaseAgent）
- [ ] 前端: P0 组件单元测试（LoginForm, RegisterForm, CommentInput, CommentList）
- [ ] 前端: P0 Composables（useArticleDetail, useValidation, auth）
- [ ] 前端: E2E 扩展（文章完整流程、AI 对话）

### Sprint 2 (第 3-4 周): 深度覆盖

- [ ] 后端: Service 层增强测试
- [ ] 后端: Agent 系统 P0 测试（ReActAgent, Memory, Security）
- [ ] 后端: 基础设施测试（RedisLock, AesUtil, WebSocket）
- [ ] 前端: P1 组件单元测试（Pagination, SearchBar, MarkdownRenderer 等）
- [ ] 前端: P1 Composables 和工具函数
- [ ] 前端: E2E 扩展（评论系统、个人中心）

### Sprint 3 (第 5-6 周): 全面覆盖

- [ ] 后端: 集成测试（完整用户流程、文章流程、AI 对话流程）
- [ ] 后端: Agent 系统 P1 测试
- [ ] 后端: 性能测试（N+1 验证、并发测试）
- [ ] 前端: P2/P3 组件单元测试（Agent 组件、技术组件）
- [ ] 前端: E2E 完整覆盖（后台管理、响应式、无障碍）
- [ ] 前后端: CI/CD 集成、覆盖率报告

---

## 七、测试规范

### 7.1 后端测试命名规范

```java
// 文件命名: {ClassName}Test.java
@ExtendWith(MockitoExtension.class)
@DisplayName("{ClassName} 单元测试")
class {ClassName}Test {

    @Nested
    @DisplayName("方法名 - 正常流程")
    class MethodNameSuccessTests {
        @Test
        @DisplayName("应该{预期结果}当{条件}")
        void should_ExpectedResult_When_Condition() {
            // Given / Arrange
            // When / Act
            // Then / Assert
        }
    }

    @Nested
    @DisplayName("方法名 - 异常流程")
    class MethodNameErrorTests {
        @Test
        @DisplayName("应该抛出{异常}当{条件}")
        void shouldThrow_Exception_When_Condition() {
            // ...
        }
    }
}
```

### 7.2 前端测试命名规范

```javascript
describe('{ComponentName} 组件', () => {
  describe('渲染', () => {
    it('应该正确渲染{关键元素}')
    it('应该在{条件}下渲染{元素}')
  })

  describe('交互', () => {
    it('点击{元素}应该触发{事件}')
    it('输入{值}应该{预期结果}')
  })

  describe('边界条件', () => {
    it('应该在空数据下正确渲染')
    it('应该在错误状态下显示{提示}')
  })
})
```

### 7.3 E2E 测试规范

```javascript
// 文件命名: {feature}.spec.js
test.describe('{功能名称}', () => {
  test.beforeEach(async ({ page }) => {
    // 通用前置操作
  })

  test('应该完成{用户故事}', async ({ page }) => {
    // Given: 前置状态
    await page.goto('/path')
    // When: 用户操作
    await page.fill('[data-testid="input"]', 'value')
    await page.click('[data-testid="submit"]')
    // Then: 断言结果
    await expect(page.locator('[data-testid="result"]')).toBeVisible()
  })
})
```

---

## 八、风险与建议

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| AI 测试依赖外部 API | 测试不稳定 | Mock LLM 响应，使用 TestLLM |
| E2E 测试依赖后端 | 前后端联调成本高 | MSW 前端 Mock + Testcontainers 后端 Mock |
| Agent 系统测试复杂度高 | 测试编写困难 | 优先测试可观测行为，内部实现 Mock |
| 测试维护成本高 | 后续难以坚持 | 代码审查要求新代码必须附带测试 |
| 覆盖率目标不切实际 | 团队压力大 | 阶段性目标，先从 P0 开始 |
