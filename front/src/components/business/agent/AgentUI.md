# Agent UI 设计文档

## 设计理念

基于 **Linear**、**Raycast**、**Vercel** 的科技风格，打造专业、沉浸式的 AI 助手界面。

### 核心设计原则

1. **沉浸式体验** - 深色主题、玻璃拟态、发光效果
2. **信息层次清晰** - 思考过程可折叠、关键信息突出
3. **实时反馈** - 流畅动画、进度可视化
4. **专业可信** - 工具调用透明、来源可追溯

---

## 视觉风格

### 色彩系统

```css
/* 主色调 */
--tech-primary: #6366F1;        /* 靛紫色 */
--tech-accent: #60A5FA;         /* 天蓝色 */
--tech-success: #10B981;        /* 翠绿色 */
--tech-warning: #F59E0B;        /* 琥珀色 */
--tech-error: #EF4444;          /* 红色 */

/* 背景色 */
--tech-bg: #0A0A0B;             /* 主背景 */
--tech-bg-card: #16161A;        /* 卡片背景 */
--tech-bg-elevated: #1A1A1D;    /*  elevated 背景 */

/* 文本色 */
--tech-text: #E5E7EB;           /* 主文本 */
--tech-text-secondary: #F3F4F6; /* 次要文本 */
--tech-text-muted: #71717A;     /* 辅助文本 */
```

### 玻璃拟态

- 背景模糊：`backdrop-filter: blur(12px)`
- 半透明背景：`rgba(22, 22, 26, 0.8)`
- 微妙边框：`rgba(255, 255, 255, 0.08)`

### 发光效果

- 主色发光：`box-shadow: 0 0 20px rgba(99, 102, 241, 0.4)`
- 输入框聚焦：`box-shadow: 0 0 20px rgba(99, 102, 241, 0.4)`

---

## 组件架构

```
AgentChat (容器)
├── AgentHeader (头部)
├── AgentMessageList (消息列表)
│   └── AgentMessage (消息项)
│       ├── AgentAvatar (头像)
│       ├── AgentMessageContent (内容)
│       │   └── AgentMarkdown (Markdown 渲染)
│       ├── AgentThoughtProcess (思考过程)
│       │   ├── AgentIntentCard (意图卡片)
│       │   ├── AgentPlanProgress (计划进度)
│       │   ├── AgentToolCalls (工具调用)
│       │   └── AgentRagSources (RAG 来源)
│       └── AgentMessageActions (操作按钮)
├── AgentInputArea (输入区)
│   ├── AgentQuickCommands (快捷指令)
│   ├── AgentInput (输入框)
│   └── AgentSendButton (发送按钮)
└── AgentStatusBar (状态栏)
```

---

## 组件详细设计

### 1. AgentMessage (消息气泡)

**用户消息**
- 位置：右侧
- 背景：渐变 `linear-gradient(135deg, #6366F1, #8B5CF6)`
- 圆角：`12px` (左下小圆角)
- 阴影：`0 0 20px rgba(99, 102, 241, 0.3)`

**AI 消息**
- 位置：左侧
- 背景：`rgba(22, 22, 26, 0.8)` + 玻璃模糊
- 边框：`1px solid rgba(99, 102, 241, 0.1)`
- 圆角：`12px` (右下小圆角)

### 2. AgentThoughtProcess (思考过程)

**折叠状态**
- 显示：意图类型 + 进度条
- 高度：`40px`
- 背景：`rgba(10, 10, 11, 0.5)`

**展开状态**
- 显示：完整思考链
- 动画：高度展开 300ms ease

**意图卡片**
- 图标：根据意图类型变化
- 徽章：置信度百分比
- 实体：标签云展示

**工具调用卡片**
- 状态指示器：
  - 等待中：灰色脉冲
  - 执行中：蓝色旋转
  - 成功：绿色对勾
  - 失败：红色叉号
- 参数：可折叠的 JSON
- 耗时：右下角显示

### 3. AgentInputArea (输入区域)

**输入框**
- 背景：`rgba(22, 22, 26, 0.8)`
- 边框：聚焦时发光
- 圆角：`12px`
- 最小高度：`44px`
- 最大高度：`120px` (自动扩展)

**快捷指令**
- 布局：水平滚动
- 样式：小卡片，图标 + 文字
- 交互：点击填充输入框

**发送按钮**
- 默认：圆形，主色背景
- 加载中：旋转动画
- 禁用：透明度降低

### 4. 动画规范

**消息进入**
```css
@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
/* 时长：300ms，缓动：ease-out */
```

**思考过程展开**
```css
transition: height 300ms ease, opacity 200ms ease;
```

**打字光标**
```css
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
/* 时长：1s，循环 */
```

**加载动画**
- 三个点的弹性动画
- 依次延迟 0.1s

---

## 响应式设计

### 桌面端 (>768px)
- 消息最大宽度：70%
- 侧边距：24px
- 输入框固定底部

### 移动端 (<768px)
- 消息最大宽度：85%
- 侧边距：12px
- 全屏抽屉

---

## 交互细节

### 悬停效果
- 消息气泡：轻微上浮 + 阴影增强
- 按钮：背景色变化 + 缩放 1.02
- 链接：下划线 + 颜色变化

### 点击反馈
- 按钮：缩放 0.98
- 卡片：边框高亮

### 滚动行为
- 平滑滚动到底部
- 新消息自动滚动
- 用户滚动时暂停自动滚动

---

## 无障碍设计

- 支持键盘导航
- 支持屏幕阅读器
- 尊重 `prefers-reduced-motion`
- 足够的颜色对比度
