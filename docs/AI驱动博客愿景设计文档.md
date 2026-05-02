# AI驱动博客系统 - 完整架构设计文档

> 版本: 2.0
> 更新: 2026-04-29
> 状态: 演进中

---

## 一、项目概述

### 1.1 愿景

**AI驱动博客** = 一个"会自己发现增长机会并执行"的博客系统

核心目标：让博客系统具备自我进化的能力，通过数据驱动 + AI执行实现自动增长。

### 1.2 核心价值

```
人驱动系统 (传统)                    AI驱动系统 (目标)
─────────────────────────────────────────────────────────
想写什么 → 自己想                  自己：分析用户在搜什么
自己写文章                        发现哪些内容缺失
自己做SEO                         自动生成文章
自己发现问题                      自动优化旧内容
自己分析                          自动提升流量
─────────────────────────────────────────────────────────
本质：人驱动                      本质：数据驱动 + AI执行
```

---

## 二、增长闭环核心

### 2.1 闭环流程

```
用户行为数据
     ↓
AI分析（找机会）
     ↓
AI生成内容 / 优化内容
     ↓
发布
     ↓
获得更多流量
     ↓
产生新数据
     ↓
继续优化
     ↓
循环...
```

### 2.2 AI具体职责

| 功能 | 说明 |
|------|------|
| 自动发现"该写什么" | AI分析搜索趋势，发现内容缺口 |
| 自动写文章 | 基于主题生成SEO优化文章 |
| 自动优化旧文章 | 发现问题并自动改进 |
| 自动做SEO | 关键词优化、标题优化、结构优化 |
| 自动回答用户问题 | 基于博客内容提供智能问答 |

---

## 三、系统架构

### 3.1 全景图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AI 驱 动 博 客 系 统                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                          🔄 增 长 闭 环 引 擎                          │   │
│   │                                                                             │   │
│   │    用户行为数据                                                               │   │
│   │         ↓                                                                    │   │
│   │    【BehaviorAnalyzer】 ─────────────────────────────────────┐               │   │
│   │         ↓                                                    │               │   │
│   │    【OpportunityFinder】 ────────────────────────────────┐    │               │   │
│   │         ↓                                         ↓      │               │   │
│   │    【ContentWriter】                            【SEO Optimizer】               │   │
│   │         ↓                                         │      │               │   │
│   │    【ContentOptimizer】◄──────────────────────────┘      │               │   │
│   │         ↓                                                    │               │   │
│   │    发布 ──► 获得流量 ──► 新数据 ──► 回到起点                  │               │   │
│   │                                                            ↓               │   │
│   │    ┌────────────────────────────────────────────────────────┘               │   │
│   │    ↓                                                                       │   │
│   │    【GrowthReActOrchestrator】 ←── 核心推理引擎 (ReAct 模式)                 │   │
│   └─────────────────────────────────────────────────────────────────────────────┘   │
│                                      ↓                                               │
│   ┌─────────────────────────────────────────────────────────────────────────────┐   │
│   │                            👤 用 户 交 互 层                                  │   │
│   │                                                                             │   │
│   │   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │   │
│   │   │ 博客前台  │  │ AI 对话  │  │ 写作助手  │  │ 增长报告  │  │ 智能问答  │   │   │
│   │   │   博客   │  │  Assistant │  │  CoWriter │  │ Dashboard │  │   Q&A    │   │   │
│   │   └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │   │
│   └────────┼─────────────┼─────────────┼─────────────┼─────────────┼──────────┘   │
│            └─────────────┴─────────────┴─────────────┴─────────────┘              │
│                                      ↓                                               │
│   ┌─────────────────────────────────────────────────────────────────────────────┐   │
│   │                           🤖 Agent 层                                         │   │
│   │                                                                             │   │
│   │   ┌─────────────────────────────────────────────────────────────────────┐   │   │
│   │   │                    GrowthReActOrchestrator                              │   │   │
│   │   │                         (增长闭环大脑)                                    │   │   │
│   │   │  ┌───────────────────────────────────────────────────────────────┐   │   │   │
│   │   │  │  🔁 True ReAct 推理循环                                        │   │   │   │
│   │   │  │                                                                │   │   │   │
│   │   │  │   Thought → Action → Observation → Thought → ... → Answer   │   │   │   │
│   │   │  │                                                                │   │   │   │
│   │   │  │   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │   │   │   │
│   │   │  │   │ Behavior │  │Opportu- │  │ Content  │  │    SEO   │  │   │   │   │
│   │   │  │   │ Analyzer │→│ nity    │→│  Writer  │→│ Optimizer│  │   │   │   │
│   │   │  │   │   Agent  │  │ Finder  │  │  Agent   │  │   Agent  │  │   │   │   │
│   │   │  │   └──────────┘  └──────────┘  └──────────┘  └──────────┘  │   │   │   │
│   │   │  └───────────────────────────────────────────────────────────────┘   │   │   │
│   │   └─────────────────────────────────────────────────────────────────────┘   │   │
│   │                                                                             │   │
│   │   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │   │
│   │   │OpportunityFinder│  │ContentWriter│  │   QA Agent  │  │  SEO Agent  │       │   │
│   │   │    Agent    │  │    Agent    │  │             │  │             │       │   │
│   │   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │   │
│   └──────────┼────────────────┼────────────────┼────────────────┼──────────────┘   │
│              └────────────────┴────────────────┴────────────────┘                  │
│                                            ↓                                            │
│   ┌─────────────────────────────────────────────────────────────────────────────┐   │
│   │                          🛠️ 工 具 层 (Tools)                                 │   │
│   │                                                                             │   │
│   │   ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐           │   │
│   │   │ContentOp-  │  │  Article   │  │  Article   │  │   Tavily   │           │   │
│   │   │portunity-  │  │ Generator  │  │ Optimizer  │  │   Search   │           │   │
│   │   │   Tool     │  │    Tool    │  │    Tool    │  │    Tool    │           │   │
│   │   └────────────┘  └────────────┘  └────────────┘  └────────────┘           │   │
│   │   ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐           │   │
│   │   │  Article   │  │  Category  │  │    Tag     │  │   Image    │           │   │
│   │   │   Query    │  │    Tool    │  │   Tool    │  │   Tool    │           │   │
│   │   │   Tool     │  │            │  │            │  │            │           │   │
│   │   └────────────┘  └────────────┘  └────────────┘  └────────────┘           │   │
│   └─────────────────────────────────────────────────────────────────────────────┘   │
│                                            ↓                                            │
│   ┌─────────────────────────────────────────────────────────────────────────────┐   │
│   │                          📊 数 据 层                                          │   │
│   │                                                                             │   │
│   │   ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐           │   │
│   │   │   Article  │  │   User     │  │   Search   │  │  Article   │           │   │
│   │   │   /Stats   │  │  Behavior  │  │  History   │  │  ReadStats │           │   │
│   │   └────────────┘  └────────────┘  └────────────┘  └────────────┘           │   │
│   │   ┌────────────┐  ┌────────────┐  ┌────────────┐                           │   │
│   │   │   Vector   │  │    Redis   │  │  PostgreSQL│                           │   │
│   │   │   (RAG)    │  │   Cache    │  │    DB      │                           │   │
│   │   └────────────┘  └────────────┘  └────────────┘                           │   │
│   └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、Agent 详细设计

### 4.1 Agent 职责矩阵

| Agent | 职责 | 输入 | 输出 |
|-------|------|------|------|
| **BehaviorAnalyzer** | 分析用户行为数据 | PV/UV/跳出/停留 | 行为洞察报告 |
| **OpportunityFinder** | 发现内容缺口 | 行为洞察 + 趋势 | 内容机会清单 |
| **ContentWriter** | 生成SEO优化文章 | 机会清单 + SEO要求 | 完整文章 |
| **ContentOptimizer** | 优化文章质量 | 原始文章 + SEO策略 | 优化后文章 |
| **SEOAgent** | 制定SEO策略 | 文章 + 关键词 | SEO策略文档 |
| **QAAgent** | 智能问答 | 用户问题 + 博客内容 | 回答 + 相关文章 |

### 4.2 Agent 角色定义

```
GrowthReActOrchestrator = "主编"
├── 决策：什么时机做什么
├── 编排：调用哪个Agent
└── 把控：质量审核、进度追踪

OpportunityFinder = "市场调研员"
├── 分析搜索趋势
├── 发现内容缺口
└── 评估机会优先级

ContentWriter = "专栏作家"
├── 理解写作风格
├── 产出原创内容
└── 接受批评修改

ContentOptimizer = "文字编辑"
├── 检查事实准确
├── 优化表达清晰
└── 提升可读性

SEOAgent = "SEO顾问"
├── 研究关键词
├── 规划内链结构
└── 建议技术优化

QAAgent = "客服专员"
├── 理解用户问题
├── 基于内容回答
└── 引导深入阅读
```

---

## 五、双层时间尺度架构

### 5.1 架构原则

> ⚠️ **关键洞察**: 闭环周期(数小时-数天)与ReAct实时性(秒级)存在本质矛盾

### 5.2 双层设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         双层时间尺度                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ╔══════════════════════════════════════════════════════════════╗  │
│  ║                     ⚡ 实时层 (秒级)                      ║  │
│  ║  ┌────────────────────────────────────────────────────┐   ║  │
│  ║  │  用户对话    │  智能问答  │  短内容优化  │  热点响应  │   ║  │
│  ║  │  ReAct Loop  │  RAG检索   │  标题优化   │  突发话题  │   ║  │
│  ║  └────────────────────────────────────────────────────┘   ║  │
│  ║                          ↓ 直接响应 (<1min)                ║  │
│  ╚══════════════════════════════════════════════════════════════╝  │
│                                    ↓                              │
│  ╔══════════════════════════════════════════════════════════════╗  │
│  ║                     🐢 异步层 (小时/天级)                   ║  │
│  ║  ┌────────────────────────────────────────────────────┐   ║  │
│  ║  │  定时任务调度器 (ScheduleTrigger)                   │   ║  │
│  ║  │                                                    │   ║  │
│  ║  │  cron: "0 0 2 * * ?"   → 每日行为分析             │   ║  │
│  ║  │  cron: "0 0 3 * * ?"   → 机会发现                 │   ║  │
│  ║  │  cron: "0 0 4 * * ?"   → 长文生成                 │   ║  │
│  ║  │  cron: "0 0 5 * * ?"   → SEO优化                 │   ║  │
│  ║  │                                                    │   ║  │
│  ║  │  事件触发:                                         │   ║  │
│  ║  │  • 文章发布 → 通知搜索引擎                        │   ║  │
│  ║  │  • 流量突增 → 触发热点分析                        │   ║  │
│  ║  │  • 排名下降 → 触发优化提醒                        │   ║  │
│  ║  └────────────────────────────────────────────────────┘   ║  │
│  ╚══════════════════════════════════════════════════════════════╝  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 六、多Agent协作模式

### 6.1 编排策略

| 模式 | 适用场景 | 示例 |
|------|---------|------|
| **顺序 (Sequential)** | 步骤清晰、线性依赖 | 机会发现 → 内容生成 → SEO优化 |
| **并行 (Concurrent)** | 独立任务、可同时执行 | 多个机会同时生成内容 |
| **群聊 (Group Chat)** | 需要多视角讨论 | 复杂主题策略制定 |
| **移交 (Handoff)** | 专长切换 | 内容生成 → 遇到代码问题 → 移交技术编辑 |

### 6.2 协作示例：复杂主题处理

```
OpportunityFinder发现高潜力复杂主题
           ↓
发起"内容策略群聊"
           ↓
┌──────────────────────────────────────┐
│        Group Chat                    │
│                                      │
│  ContentWriter: "这个主题需要..."    │
│  SEOAgent: "关键词布局建议..."       │
│  ContentOptimizer: "需要补充案例..." │
│                                      │
└──────────────────────────────────────┘
           ↓
     达成共识 → 分配任务
```

---

## 七、反思与自我进化机制

### 7.1 "演员-评论家"分离设计

```
┌─────────────────────────────────────────────────────────────┐
│                    演员-评论家 模式                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌─────────────────┐         ┌─────────────────┐          │
│   │    演员          │         │    评论家        │          │
│   │  ContentWriter   │         │  CriticAgent    │          │
│   │                 │         │                 │          │
│   │  执行内容生成    │←───────→│  审视与反馈      │          │
│   │                 │         │                 │          │
│   └─────────────────┘         └─────────────────┘          │
│          ↑                              ↓                    │
│          │                              │                    │
│          └──────────────────────────────┘                    │
│                        ↓                                    │
│              迭代优化，直到满意                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 反思机制

```java
// 行动后反思
public class ReflectionMechanism {

    // 生成内容后立即自评
    public ReflectionResult reflect(GeneratedContent content) {
        List<ReflectionQuestion> questions = List.of(
            new ReflectionQuestion("主题是否偏离？"),
            new ReflectionQuestion("事实是否准确？"),
            new ReflectionQuestion("风格是否一致？"),
            new ReflectionQuestion("是否有遗漏关键点？")
        );

        return questions.stream()
            .map(q -> evaluate(content, q))
            .reduce(new ReflectionResult(), (a, b) -> a.merge(b));
    }

    // 沉淀经验到向量数据库
    public void storeReflectionNote(ReflectionNote note) {
        vectorStore.add(note.toEmbedding());
    }

    // 检索相似经验
    public List<ReflectionNote> retrieveSimilar(String taskType) {
        return vectorStore.search(taskType);
    }
}
```

---

## 八、成本与延迟优化

### 8.1 KV缓存优化策略

> 💡 **核心洞察**: KV缓存命中率是Agent系统最重要的单一性能指标

### 8.2 优化原则

| 原则 | 说明 | 示例 |
|------|------|------|
| **Prompt前缀稳定** | 避免动态字段破坏缓存 | 移除时间戳等动态内容 |
| **掩码而非移除** | 预置所有工具，通过掩码控制可用性 | 保持工具列表稳定 |
| **上下文复用** | 共享检索到的上下文 | 多Agent共享知识库检索结果 |

### 8.3 实现示例

```java
// 坏的实践：每次请求动态添加时间戳
systemPrompt = "当前时间：" + new Date() + "，你是..."  // ❌ 破坏KV缓存

// 好的实践：移除动态字段
systemPrompt = "你是博客写作助手。"  // ✅ KV缓存可复用

// 掩码而非移除
public class ToolMaskingStrategy {

    // 预置所有工具
    private static final List<Tool> ALL_TOOLS = List.of(
        researchTool, writingTool, editingTool,
        publishingTool, seoTool, analysisTool
    );

    // 通过掩码控制可用工具
    public List<Tool> getAvailableTools(TaskContext context) {
        return ALL_TOOLS.stream()
            .filter(tool -> !context.isMasked(tool.getName()))
            .collect(toList());
    }
}
```

---

## 九、安全护栏与人机协同

### 9.1 多层护栏设计

```
┌─────────────────────────────────────────────────────────────┐
│                      安全护栏架构                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                   输入护栏                            │  │
│   │   • 过滤不当内容                                     │  │
│   │   • 敏感词检测                                       │  │
│   │   • 恶意输入拦截                                     │  │
│   └─────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                   行为护栏                            │  │
│   │   • 限制高风险操作 (发布/删除)                        │  │
│   │   • 操作审计日志                                     │  │
│   │   • 权限控制                                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                   输出护栏                            │  │
│   │   • 格式校验                                         │  │
│   │   • 内容安全检查                                     │  │
│   │   • 质量门禁                                         │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 9.2 Human-in-the-Loop 节点

| 节点类型 | 触发条件 | 人工操作 |
|---------|---------|---------|
| **发布前审核** | 高风险内容 | 批准/修改/拒绝 |
| **策略确认** | 大规模修改计划 | 确认执行 |
| **异常处理** | Agent无法决策 | 介入指导 |

---

## 十、有状态工作流与断点续跑

### 10.1 检查点机制

```java
@Service
public class CheckpointManager {

    // 在关键步骤后创建快照
    public void createCheckpoint(WorkflowExecution execution, String stepName) {
        Checkpoint checkpoint = Checkpoint.builder()
            .executionId(execution.getId())
            .stepName(stepName)
            .state(execution.getCurrentState())
            .timestamp(Instant.now())
            .build();

        checkpointStore.save(checkpoint);
    }

    // 从检查点恢复
    public WorkflowExecution restore(String executionId) {
        Checkpoint latest = checkpointStore.findLatest(executionId);
        return rebuildState(latest);
    }
}
```

### 10.2 动态路由

```
                    ┌──────────────────┐
                    │  LLM 节点输出     │
                    └────────┬─────────┘
                             ↓
              ┌──────────────────────────────┐
              │      路由判断                 │
              │  risk_level > 0.8 ?          │
              └──────────────────────────────┘
                    ↓                 ↓
                   Yes                No
                    ↓                 ↓
         ┌──────────────────┐  ┌──────────────────┐
         │ human_approval   │  │   继续执行       │
         │   人工审核节点    │  │                  │
         └──────────────────┘  └──────────────────┘
```

---

## 十一、多阶段启动模式

### 11.1 冷启动策略

```
阶段1: 人工驱动期 (Day 0 - Day 7)
───────────────────────────────────────
用户行为数据: 0%
外部信号: 80%
人工输入: 20%

策略:
• 使用 Google Trends / 百度指数 找热门话题
• 竞品分析 (模仿做得好的博客)
• 用户手动输入"想写的主题池"
• 生成首批内容 (10-20篇)

阶段2: 数据积累期 (Day 7 - Day 30)
───────────────────────────────────────
用户行为数据: 30%
外部信号: 50%
人工反馈: 20%

策略:
• 开始追踪: 搜索词、页面停留、跳出率
• 验证阶段1内容效果
• 逐步调整主题池

阶段3: 智能驱动期 (Day 30+)
───────────────────────────────────────
用户行为数据: 60%
外部信号: 25%
人工反馈: 15%

策略:
• BehaviorAnalyzer 权重增加
• 机会发现更多依赖数据
• 内容与用户需求更匹配
```

---

## 十二、内容审核三层机制

### 12.1 分层设计

```
┌─────────────────────────────────────────────────────────────┐
│                      三层审核机制                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Layer 1: 自动检查 (快速, <10s)                     │  │
│  │                                                          │  │
│  │  • 敏感词过滤 (Political/Sexual/Drugs/Gamble)        │  │
│  │  • 格式检查 (字数/标题/代码块)                       │  │
│  │  • 明显重复检测                                        │  │
│  │  • 基础SEO检查                                         │  │
│  │                                                          │  │
│  │  Result: PASS / FAIL / NEED_HUMAN_REVIEW             │  │
│  └─────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Layer 2: AI辅助检查 (中等, 30s-2min)              │  │
│  │                                                          │  │
│  │  • 事实性校验 (搜索验证关键声明)                      │  │
│  │  • 风格匹配度 (与博主历史文章对比)                   │  │
│  │  • 代码正确性 (语法/可运行性)                        │  │
│  │  • 引用来源检查 (是否有依据)                         │  │
│  │                                                          │  │
│  │  Result: SAFE / CAUTION / REJECT                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Layer 3: 人工审核 (慢速, 人工介入)                   │  │
│  │                                                          │  │
│  │  • 高风险内容 (医疗/金融/法律)                        │  │
│  │  • 敏感话题                                            │  │
│  │  • 用户投诉内容                                        │  │
│  │  • 模型不确定内容                                       │  │
│  │                                                          │  │
│  │  Result: APPROVED / REJECTED                        │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 十三、职责分层 (SEO Agent vs ContentOptimizer)

### 13.1 策略层 vs 执行层

```
┌─────────────────────────────────────────────────────────────┐
│                      职责分层设计                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                   SEO Agent (策略层)                    │  │
│  │                                                          │  │
│  │  职责:                                                   │  │
│  │  • 关键词研究 & 优先级排序                             │  │
│  │  • 内链策略规划                                        │  │
│  │  • 结构化数据建议 (Schema.org)                        │  │
│  │  • 竞品对比分析                                        │  │
│  │  • 搜索意图理解                                        │  │
│  │                                                          │  │
│  │  输出:                                                   │  │
│  │  • SEO策略文档 (关键词列表 + 优先级 + 内链方案)        │  │
│  │  • 优化建议清单                                        │  │
│  │                                                          │  │
│  │  不做: 不直接修改文章内容                              │  │
│  └───────────────────────────────┬─────────────────────────┘  │
│                                  ↓                              │
│  ┌─────────────────────────────────────────────────────┐  │
│  │              ContentOptimizer (执行层)                 │  │
│  │                                                          │  │
│  │  职责:                                                   │  │
│  │  • 接收SEO策略，执行具体修改                            │  │
│  │  • 标题重构 (基于SEO建议)                              │  │
│  │  • 关键词密度调整                                       │  │
│  │  • H标签结构重排                                       │  │
│  │  • 内链插入 (根据策略)                                 │  │
│  │  • 图片Alt优化                                          │  │
│  │                                                          │  │
│  │  输入: Article + SEO策略文档                           │  │
│  │  输出: 优化后文章                                       │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 十四、技术栈

### 14.1 后端技术

| 组件 | 技术 | 用途 |
|------|------|------|
| 框架 | Spring Boot 3.4 | 核心框架 |
| 语言 | Java 17+ | 开发语言 |
| ORM | MyBatis Plus | 数据访问 |
| 缓存 | Redis | 缓存层 |
| 消息 | RabbitMQ | 异步消息 |
| 向量 | OpenSearch | RAG知识库 |
| 认证 | Sa-Token | 权限控制 |
| 限流 | Resilience4j | 稳定性 |

### 14.2 AI层

| 组件 | 技术 | 用途 |
|------|------|------|
| LLM | OpenAI/GLM/Baidu | 大语言模型 |
| Embedding | BGE/Text2Vec | 向量化 |
| 推理 | ReAct Engine | Agent推理 |
| 提示 | Prompt Templates | 提示工程 |

### 14.3 前端技术

| 组件 | 技术 | 用途 |
|------|------|------|
| 框架 | Vue 3 | UI框架 |
| 构建 | Vite | 打包工具 |
| 状态 | Pinia | 状态管理 |
| UI | Arco Design | 组件库 |
| 通信 | SSE | 流式输出 |

---

## 十五、文件结构

```
com.xingchen.backend
├── agent/
│   ├── react/                          ✅ 已实现
│   │   ├── ReActAgent.java
│   │   ├── ReActAgentWithFunctionCalling.java
│   │   ├── ReActState.java
│   │   ├── ReActPrompt.java
│   │   ├── ReActParser.java
│   │   └── ReActProperties.java
│   │
│   ├── growth/                          🔄 规划中
│   │   ├── GrowthReActOrchestrator.java
│   │   ├── BehaviorAnalyzerAgent.java
│   │   ├── OpportunityFinderAgent.java
│   │   ├── ContentWriterAgent.java
│   │   ├── ContentOptimizerAgent.java
│   │   ├── SEOAgent.java
│   │   ├── QAAgent.java
│   │   ├── CriticAgent.java             # 评论家
│   │   └── ReflectionAgent.java          # 反思机制
│   │
│   └── workflow/                         🔄 规划中
│       ├── WorkflowEngine.java
│       ├── WorkflowDefinition.java
│       ├── WorkflowExecution.java
│       ├── CheckpointManager.java
│       └── HumanApprovalHandler.java
│
├── audit/                               🔄 规划中
│   ├── ContentAuditorAgent.java
│   ├── FactChecker.java
│   ├── StyleAnalyzer.java
│   └── SensitiveWordFilter.java
│
├── config/
│   ├── GuardrailsConfig.java             # 安全护栏
│   └── WorkflowConfig.java
│
└── scheduler/
    └── GrowthScheduler.java              # 异步调度
```

---

## 十六、API 设计

### 16.1 核心API

```
# ReAct 对话
POST /api/ai/react/chat          - 智能对话
POST /api/ai/react/chat/stream   - 流式输出

# 机会发现
GET  /api/ai/opportunity/discover - 机会发现
GET  /api/ai/opportunity/gaps     - 内容缺口分析

# 内容生成
POST /api/ai/content/generate     - 生成文章
POST /api/ai/content/optimize    - 优化文章

# 智能问答
POST /api/ai/qa/answer           - 智能问答

# 增长系统
GET  /api/growth/report         - 增长报告
POST /api/growth/execute         - 执行增长任务

# 工作流
GET  /api/workflow/{id}         - 查询执行状态
POST /api/workflow/{id}/resume   - 恢复执行
POST /api/workflow/{id}/approve  - 人工批准
```

---

## 十七、框架选型参考

| 框架 | 核心优势 | 适用场景 | 对本项目价值 |
|------|---------|---------|-------------|
| **LangGraph** | 有向图工作流、状态持久化、断点续跑 | 长时间运行、复杂可控的生产级任务 | ⭐⭐⭐ 适合增长闭环大脑 |
| **AutoGen** | 对话协作、角色扮演、群聊 | 多视角开放性任务 | ⭐⭐ 动态群聊设计 |
| **CrewAI** | 角色驱动、极简API、快速构建 | 流程固定的内容生产管道 | ⭐⭐ 快速验证 |
| **OpenAI SDK** | 原生集成、护栏保障、极简API | 快速原型和简单应用 | ⭐ 顶层设计理念 |

---

## 十八、实施路线图

### Phase 1: 基础建设 (1-2天)
- [ ] 集成 ReActAgent 到 GrowthOrchestrator
- [ ] 创建 GrowthReActOrchestrator
- [ ] 实现基础工作流引擎

### Phase 2: 核心Agent (2-3天)
- [ ] BehaviorAnalyzerAgent
- [ ] OpportunityFinderAgent
- [ ] ContentWriterAgent

### Phase 3: 优化Agent (2-3天)
- [ ] SEOAgent (策略层)
- [ ] ContentOptimizerAgent (执行层)
- [ ] 职责分离实现

### Phase 4: 安全与质量 (2-3天)
- [ ] ContentAuditorAgent (三层审核)
- [ ] 安全护栏配置
- [ ] Human-in-the-Loop 节点

### Phase 5: 进化机制 (2-3天)
- [ ] CriticAgent (评论家)
- [ ] ReflectionAgent (反思机制)
- [ ] 经验沉淀到向量数据库

### Phase 6: 监控与优化 (1-2天)
- [ ] 可观测性仪表盘
- [ ] KV缓存优化
- [ ] 性能基准测试

---

## 十九、潜在问题与解决方案

| 问题 | 解决方案 | 优先级 |
|------|---------|--------|
| 闭环周期与ReAct矛盾 | 双层架构 (实时+异步) | P0 |
| ContentOptimizer与SEO重叠 | 策略层/执行层分离 | P1 |
| 冷启动数据匮乏 | 三阶段启动模式 | P1 |
| Orchestrator瓶颈 | 去中心化 + Workflow持久化 | P0 |
| 内容质量风险 | 三层审核机制 | P0 |

---

## 二十、ReWOO 批量执行模式

### 20.1 模式概述

> 💡 **核心理念**: 与其一步步等待工具结果再思考（ReAct模式），不如先一口气把任务拆解成多个Planner任务再批量执行。

**适用场景**:
- 依赖链清晰的子任务
- 工具调用之间无数据依赖
- 可并行执行的独立任务

### 20.2 ReAct vs ReWOO 对比

```
┌─────────────────────────────────────────────────────────────────┐
│                     ReAct 模式 (同步等待)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Step 1: LLM → Thought → Action (等待)                       │
│                    ↓                                           │
│           Observation ← 工具返回                                 │
│                    ↓                                           │
│   Step 2: LLM → Thought → Action (等待)                       │
│                    ↓                                           │
│           Observation ← 工具返回                                 │
│                    ↓                                           │
│   Step 3: LLM → Thought → Final Answer                        │
│                                                                  │
│   特点: 串行执行，每步等待，可动态调整                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     ReWOO 模式 (批量执行)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Phase 1: 规划器                                               │
│   LLM → 生成完整执行计划                                         │
│   [Tool A(params)] → [Tool B(params)] → [Tool C(params)]     │
│                                                                  │
│   Phase 2: 批量执行                                              │
│   Executor → 并行/串行执行所有工具                               │
│   Tool A ──┐                                                    │
│   Tool B ──┼──→ 收集所有结果                                    │
│   Tool C ──┘                                                    │
│                                                                  │
│   Phase 3: 最终答案                                              │
│   LLM → 基于所有结果生成答案                                     │
│                                                                  │
│   特点: 高效，适合工具调用模式固定的任务                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 20.3 混合策略

```
┌─────────────────────────────────────────────────────────────────┐
│                     混合执行策略                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ContentWriter 任务:                                            │
│   ─────────────────────────────────────────────────────────     │
│   1. 先用 ReAct 探索: "用户想要什么风格的博客?"                │
│      → 动态交互，确定写作方向                                   │
│                                                                  │
│   2. 确定后用 ReWOO 批量:                                       │
│      → 批量调用: 搜索相关资料 + 获取SEO关键词 + 查询竞品文章    │
│      → 并行执行，汇总结果                                       │
│                                                                  │
│   3. 再用 ReAct 撰写:                                           │
│      → 基于汇总结果，动态撰写和调整                              │
│                                                                  │
│   4. 最后用 ReWOO 发布:                                         │
│      → 批量调用: 保存草稿 + 更新标签 + 提交搜索引擎              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 20.4 实现示例

```java
@Service
public class ReWOOModeExecutor {

    public ReWOOResult execute(String task, List<Tool> tools) {
        // Phase 1: 规划器 - 分解任务
        ExecutionPlan plan = planner.createPlan(task, tools);

        // Phase 2: 批量执行
        Map<String, Object> results = executor.executeAll(plan.getSteps());

        // Phase 3: 生成答案
        String answer = llm.generate(task, results);

        return ReWOOResult.builder()
            .plan(plan)
            .results(results)
            .answer(answer)
            .build();
    }
}
```

---

## 二十一、Tool Calling 优化技巧

### 21.1 核心原则

| 原则 | 说明 | 效果 |
|------|------|------|
| **稳定性** | 工具列表保持稳定，避免动态修改 | 提升KV缓存命中率 |
| **完整性** | 预置所有可能用到的工具 | 减少漏判 |
| **可观测** | 工具调用要有明确日志 | 便于调试 |

### 21.2 工具描述规范

```java
// 好的工具描述
public class ToolDescriptor {

    public static ToolDescriptor articleSearchTool() {
        return ToolDescriptor.builder()
            .name("article_search")
            .description("搜索博客文章数据库，返回相关文章列表")
            .category("content")
            .inputSchema(Schema.builder()
                .type("object")
                .properties(Map.of(
                    "keyword", SchemaProperty.builder()
                        .type("string")
                        .description("搜索关键词")
                        .required(true)
                        .build(),
                    "limit", SchemaProperty.builder()
                        .type("integer")
                        .description("返回结果数量限制")
                        .defaultValue(10)
                        .build()
                ))
                .required(List.of("keyword"))
                .build())
            .examples(List.of(
                "article_search(keyword='Spring Boot', limit=5)",
                "article_search(keyword='Docker 部署')"
            ))
            .riskLevel(RiskLevel.LOW)  // 低风险操作
            .requiresApproval(false)
            .build();
    }

    // 高风险操作
    public static ToolDescriptor publishTool() {
        return ToolDescriptor.builder()
            .name("article_publish")
            .description("发布文章到博客平台")
            .category("publishing")
            .riskLevel(RiskLevel.HIGH)  // 高风险操作
            .requiresApproval(true)      // 需要人工批准
            .build();
    }
}
```

### 21.3 掩码策略

```java
@Service
public class ToolMaskingService {

    // 预置所有工具
    private static final List<Tool> ALL_TOOLS = List.of(
        // 内容相关
        articleSearchTool(),
        articleGeneratorTool(),
        articleOptimizerTool(),

        // SEO相关
        keywordResearchTool(),
        seoAnalyzerTool(),
        metaTagGeneratorTool(),

        // 发布相关
        draftSaveTool(),
        publishTool(),
        submitToSearchEngineTool(),

        // 搜索相关
        webSearchTool(),
        competitorAnalysisTool(),

        // 审核相关
        contentAuditorTool(),
        factCheckerTool()
    );

    // 获取当前上下文可用的工具 (掩码)
    public List<Tool> getAvailableTools(AgentContext context) {
        Set<String> maskedTools = calculateMask(context);

        return ALL_TOOLS.stream()
            .filter(tool -> !maskedTools.contains(tool.getName()))
            .collect(toList());
    }

    // 计算需要掩码的工具
    private Set<String> calculateMask(AgentContext context) {
        Set<String> masked = new HashSet<>();

        // 用户未认证，掩码发布工具
        if (!context.isAuthenticated()) {
            masked.add("article_publish");
            masked.add("draft_save");
        }

        // 审核未通过，掩码发布工具
        if (!context.isContentApproved()) {
            masked.add("article_publish");
        }

        // 信用不足，掩码高消耗工具
        if (context.getCredits() < 10) {
            masked.add("competitor_analysis");
            masked.add("batch_generate");
        }

        // 非管理员，掩码系统管理工具
        if (!context.isAdmin()) {
            masked.add("system_config");
            masked.add("user_management");
        }

        return masked;
    }
}
```

---

## 二十二、提示工程最佳实践

### 22.1 Prompt 模板设计原则

```
┌─────────────────────────────────────────────────────────────────┐
│                    Prompt 模板设计原则                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. 角色清晰 (Role Clarity)                                    │
│     "你是一位专注于技术博客的资深编辑，具有10年经验"           │
│                                                                  │
│  2. 任务明确 (Task Specificity)                                │
│     "请撰写一篇关于XX主题的博客文章，要求:..."                 │
│                                                                  │
│  3. 格式约束 (Output Format)                                    │
│     "请按以下JSON格式输出: {title, content, summary, tags}"   │
│                                                                  │
│  4. 例子引导 (Few-shot Examples)                                │
│     "例如: 标题: 'Spring Boot 3.0 实战指南'"                  │
│                                                                  │
│  5. 约束说明 (Constraints)                                      │
│     "不要使用技术术语，保持通俗易懂"                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 22.2 分层 Prompt 模板

```java
@Service
public class PromptTemplateService {

    // 系统级 Prompt (固定，不含动态内容)
    public String getSystemPrompt(AgentType agentType) {
        return switch (agentType) {
            case CONTENT_WRITER -> """
                你是一位专注于技术博客的资深内容创作者。

                写作原则:
                1. 深入浅出，让技术小白也能理解
                2. 结合实际案例，有可操作性
                3. 逻辑清晰，结构严谨
                4. 适当使用代码示例

                文章结构:
                - 引人入胜的开头
                - 问题背景
                - 核心内容
                - 实战案例
                - 总结与展望

                输出格式:
                {
                    "title": "文章标题",
                    "summary": "200字摘要",
                    "content": "完整文章内容",
                    "tags": ["标签1", "标签2"],
                    "keywords": ["关键词1", "关键词2"]
                }
                """;

            case SEO_AGENT -> """
                你是一位SEO优化专家，擅长提升博客文章的搜索引擎排名。

                优化原则:
                1. 标题包含核心关键词，吸引点击
                2. 合理分布关键词，密度2-5%
                3. 使用H1/H2/H3结构化内容
                4. 添加相关内链和外链
                5. Meta描述要吸引人

                输出格式:
                {
                    "suggested_title": "优化后的标题",
                    "suggested_description": "Meta描述",
                    "keywords": ["关键词列表"],
                    "heading_suggestions": ["H2标题1", "H2标题2"],
                    "internal_links": [{"anchor": "锚文本", "target": "目标URL"}]
                }
                """;

            default -> throw new IllegalArgumentException("Unknown agent type");
        };
    }

    // 用户级 Prompt (动态生成)
    public String getUserPrompt(ContentTask task) {
        return String.format("""
            请为以下主题撰写一篇博客文章:

            主题: %s
            目标读者: %s
            风格要求: %s
            字数要求: %d 字
            关键词: %s

            背景信息:
            %s
            """,
            task.getTopic(),
            task.getTargetAudience(),
            task.getStyleRequirement(),
            task.getWordCount(),
            String.join(", ", task.getKeywords()),
            task.getBackgroundInfo()
        );
    }

    // 上下文 Prompt (用于多轮对话)
    public String getContextualPrompt(AgentContext context, String currentTask) {
        StringBuilder sb = new StringBuilder();

        // 添加对话历史
        if (!context.getConversationHistory().isEmpty()) {
            sb.append("对话历史:\\n");
            context.getConversationHistory().forEach(msg ->
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\\n")
            );
            sb.append("\\n");
        }

        // 添加相关记忆
        if (context.getRelevantMemories() != null && !context.getRelevantMemories().isEmpty()) {
            sb.append("相关记忆:\\n");
            context.getRelevantMemories().forEach(mem ->
                sb.append("- ").append(mem.getContent()).append("\\n")
            );
            sb.append("\\n");
        }

        sb.append("当前任务: ").append(currentTask);

        return sb.toString();
    }
}
```

### 22.3 KV缓存优化

```java
@Service
public class KVCacheOptimizer {

    // 坏的实践: 动态时间戳破坏缓存
    public String BAD_SYSTEM_PROMPT = """
        当前时间: """ + new Date() + """
        你是一个博客写作助手...
        """;  // ❌ 每次都不同，KV缓存失效

    // 好的实践: 移除动态内容
    public String GOOD_SYSTEM_PROMPT = """
        你是一个博客写作助手，专注于技术类文章的创作。
        """;  // ✅ 固定内容，KV缓存可复用

    // 动态内容通过参数传入
    public String buildPrompt(String dynamicTime, String userTask) {
        // systemPrompt 固定，可缓存
        // userTask 动态，每次不同
        return String.format("""
            [系统上下文 - 固定]
            你是一个博客写作助手。

            [用户任务 - 动态]
            %s

            [当前时间参考]
            %s
            """, userTask, dynamicTime);
    }
}
```

---

## 二十三、性能基准测试

### 23.1 测试框架

```java
@Service
public class AgentBenchmarkService {

    @Autowired private GrowthOrchestrator orchestrator;
    @Autowired private MeterRegistry meterRegistry;

    // 标准测试集
    private static final List<BenchmarkTask> STANDARD_TASKS = List.of(
        BenchmarkTask.builder()
            .name("full_article_generation")
            .description("完整文章生成流程")
            .task(ContentTask.builder()
                .topic("Docker容器化部署Spring Boot应用")
                .wordCount(2000)
                .keywords(List.of("Docker", "Spring Boot", "部署"))
                .build())
            .expectedSteps(5)
            .maxDuration(Duration.ofMinutes(5))
            .build(),

        BenchmarkTask.builder()
            .name("content_optimization")
            .description("内容优化流程")
            .task(OptimizationTask.builder()
                .articleId(1L)
                .focusAreas(List.of("title", "keywords", "structure"))
                .build())
            .expectedSteps(3)
            .maxDuration(Duration.ofMinutes(2))
            .build(),

        BenchmarkTask.builder()
            .name("opportunity_discovery")
            .description("机会发现流程")
            .task(DiscoveryTask.builder()
                .dataSource(DiscoveryTask.DataSource.BEHAVIOR_ANALYSIS)
                .limit(10)
                .build())
            .expectedSteps(4)
            .maxDuration(Duration.ofMinutes(3))
            .build()
    );

    // 执行基准测试
    public BenchmarkReport runBenchmark() {
        List<BenchmarkResult> results = new ArrayList<>();

        for (BenchmarkTask task : STANDARD_TASKS) {
            BenchmarkResult result = executeWithMetrics(task);
            results.add(result);
        }

        return BenchmarkReport.builder()
            .timestamp(Instant.now())
            .results(results)
            .summary(calculateSummary(results))
            .build();
    }

    // 执行并收集指标
    private BenchmarkResult executeWithMetrics(BenchmarkTask task) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            long startTime = System.currentTimeMillis();

            // 执行任务
            Object result = executeTask(task);

            long duration = System.currentTimeMillis() - startTime;
            sample.stop(meterRegistry.timer("benchmark.task",
                "name", task.getName(), "status", "success"));

            return BenchmarkResult.builder()
                .task(task)
                .status(Status.SUCCESS)
                .durationMs(duration)
                .timestamp(Instant.now())
                .build();

        } catch (Exception e) {
            sample.stop(meterRegistry.timer("benchmark.task",
                "name", task.getName(), "status", "failed"));

            return BenchmarkResult.builder()
                .task(task)
                .status(Status.FAILED)
                .error(e.getMessage())
                .timestamp(Instant.now())
                .build();
        }
    }
}
```

### 23.2 关键指标

| 指标 | 说明 | 目标值 |
|------|------|--------|
| **TTFT** | 首Token时间 | < 2s |
| **总响应时间** | 端到端完成时间 | < 目标时间的80% |
| **成功率** | 任务成功完成率 | > 95% |
| **Token消耗** | 每次调用Token数 | 监控趋势 |
| **工具调用成功率** | Tool Calling成功率 | > 99% |

---

## 二十四、可观测性最佳实践

### 24.1 三大支柱

```
┌─────────────────────────────────────────────────────────────────┐
│                      可观测性三大支柱                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │     日志        │  │     指标        │  │     链路        │ │
│  │   (Logs)       │  │  (Metrics)      │  │  (Traces)       │ │
│  │                 │  │                 │  │                 │ │
│  │  结构化日志     │  │  Counter/Gauge  │  │  OpenTelemetry  │ │
│  │  级别: ERROR   │  │  /Histogram     │  │  端到端追踪     │ │
│  │  保留: 30天    │  │  实时监控       │  │  性能分析       │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 24.2 结构化日志

```java
@Service
public class AgentLoggingService {

    // Agent 执行日志
    public void logAgentExecution(AgentExecution execution) {
        Map<String, Object> logData = Map.of(
            "event", "agent.execution",
            "agent_type", execution.getAgentType(),
            "session_id", execution.getSessionId(),
            "task_id", execution.getTaskId(),
            "duration_ms", execution.getDurationMs(),
            "status", execution.getStatus(),
            "llm_calls", execution.getLlmCallCount(),
            "tool_calls", Map.of(
                "total", execution.getTotalToolCalls(),
                "successful", execution.getSuccessfulToolCalls(),
                "failed", execution.getFailedToolCalls()
            )
        );

        logger.info(JSON.toJSONString(logData));
    }

    // 工具调用日志
    public void logToolCall(ToolCall call) {
        Map<String, Object> logData = Map.of(
            "event", "tool.call",
            "tool_name", call.getToolName(),
            "parameters", call.getParameters(),
            "duration_ms", call.getDurationMs(),
            "success", call.isSuccess(),
            "error", call.getErrorMessage()
        );

        logger.info(JSON.toJSONString(logData));
    }

    // LLM 调用日志 (含Token统计)
    public void logLLMCall(LLMCall call) {
        Map<String, Object> logData = Map.of(
            "event", "llm.call",
            "model", call.getModel(),
            "input_tokens", call.getInputTokens(),
            "output_tokens", call.getOutputTokens(),
            "total_tokens", call.getTotalTokens(),
            "duration_ms", call.getDurationMs(),
            "cost", call.getEstimatedCost(),
            "cache_hit", call.isCacheHit()
        );

        logger.info(JSON.toJSONString(logData));
    }
}
```

### 24.3 关键指标监控

```java
@Service
public class AgentMetricsService {

    private final MeterRegistry meterRegistry;

    // Agent 执行计数
    public void recordAgentExecution(String agentType, String status) {
        Counter.builder("agent.executions.total")
            .tag("agent_type", agentType)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
    }

    // LLM Token 消耗
    public void recordTokenUsage(String model, long inputTokens, long outputTokens) {
        meterRegistry.summary("llm.tokens.total",
            "model", model,
            "type", "input").record(inputTokens);

        meterRegistry.summary("llm.tokens.total",
            "model", model,
            "type", "output").record(outputTokens);
    }

    // 工具调用延迟
    public void recordToolLatency(String toolName, long durationMs) {
        meterRegistry.summary("tool.latency",
            "tool_name", toolName)
            .record(durationMs);
    }

    // 内容质量评分
    public void recordContentQualityScore(String articleId, double score) {
        Gauge.builder("content.quality.score", () -> score)
            .tag("article_id", articleId)
            .register(meterRegistry);
    }
}
```

---

## 二十五、实用代码模板

### 25.1 简洁的 Agent 基类

```java
public abstract class BaseAgent {

    protected final LLMProvider llm;
    protected final ToolRegistry tools;
    protected final AgentLogger logger;

    public BaseAgent(LLMProvider llm, ToolRegistry tools) {
        this.llm = llm;
        this.tools = tools;
        this.logger = new AgentLogger(this.getClass().getSimpleName());
    }

    // 子类实现的核心逻辑
    protected abstract AgentResult executeImpl(AgentContext context);

    // 执行入口 (模板方法)
    public final AgentResult execute(AgentContext context) {
        logger.start(context.getTaskId());

        try {
            // 1. 前置检查
            preCheck(context);

            // 2. 执行核心逻辑
            AgentResult result = executeImpl(context);

            // 3. 后置处理
            postProcess(result);

            logger.success(context.getTaskId(), result);
            return result;

        } catch (Exception e) {
            logger.error(context.getTaskId(), e);
            return AgentResult.failure(e.getMessage());
        }
    }

    protected void preCheck(AgentContext context) {
        // 权限检查
        // 参数验证
        // 限流检查
    }

    protected void postProcess(AgentResult result) {
        // 记录日志
        // 更新指标
        // 清理资源
    }
}
```

### 25.2 简单的状态机

```java
public class SimpleStateMachine<S, E> {

    private final Map<S, Map<E, Transition<S>>> transitions = new HashMap<>();

    public void addTransition(S from, E event, S to, Runnable action) {
        transitions.computeIfAbsent(from, k -> new HashMap<>())
            .put(event, new Transition<>(to, action));
    }

    public S transition(S currentState, E event) {
        Transition<S> transition = transitions.get(currentState).get(event);
        if (transition == null) {
            throw new IllegalStateException("Invalid transition: " + currentState + " + " + event);
        }

        transition.action().run();
        return transition.targetState();
    }
}

// 使用示例
public class ContentWorkflow {

    private final SimpleStateMachine<ContentState, ContentEvent> workflow;

    public ContentWorkflow() {
        workflow = new SimpleStateMachine<>();

        workflow.addTransition(
            ContentState.DRAFT,
            ContentEvent.SUBMIT_FOR_REVIEW,
            ContentState.PENDING_REVIEW,
            () -> notifyReviewers()
        );

        workflow.addTransition(
            ContentState.PENDING_REVIEW,
            ContentEvent.APPROVE,
            ContentState.APPROVED,
            () -> {}  // 审核通过
        );

        workflow.addTransition(
            ContentState.PENDING_REVIEW,
            ContentEvent.REJECT,
            ContentState.DRAFT,
            () -> notifyAuthor("需要修改")
        );
    }
}
```

---

## 二十六、关键设计忠告

### 26.1 核心原则

```
┌─────────────────────────────────────────────────────────────────┐
│                    关键设计忠告                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. 简单优于复杂                                                 │
│     → 线性流程能解决的，不要用图结构                               │
│     → 简单状态机能解决的，不要引入复杂框架                          │
│                                                                  │
│  2. 实测优于推测                                                 │
│     → 不要在没数据时优化                                          │
│     → 等有瓶颈再优化                                              │
│                                                                  │
│  3. 可用优于完美                                                 │
│     → 先跑起来                                                   │
│     → 迭代改进                                                   │
│                                                                  │
│  4. 框架为业务服务                                                │
│     → 不要为了用框架而用框架                                      │
│     → 自研能解决的不引入外部依赖                                   │
│                                                                  │
│  5. 可观测性优先                                                 │
│     → 日志、指标、链路追踪先做好                                  │
│     → 便于调试和问题定位                                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 26.2 不要过早优化

```
常见过早优化:
❌ 在没有数据时设计复杂的缓存策略
❌ 在没有性能问题时引入多级队列
❌ 在没有并发场景时设计分布式架构
❌ 在没有复杂分支时引入状态机框架
❌ 在没有多租户需求时设计租户隔离

正确的做法:
✅ 先用最简单的方式实现
✅ 监控关键指标
✅ 发现问题后针对性优化
```

---

*文档版本: 3.0 | 最后更新: 2026-04-29*
*新增章节: 二十(ReWOO) / 二十一(Tool Calling) / 二十二(Prompt) / 二十三(基准测试) / 二十四(可观测性) / 二十五(代码模板) / 二十六(设计忠告)*
