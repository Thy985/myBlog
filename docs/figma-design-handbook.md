# XingChen博客系统 - 完整UI设计手稿

> **项目名称**: XingChen博客系统  
> **设计风格**: 现代科技风（Glassmorphism + Neon Glow）  
> **技术栈**: Vue 3 + Element Plus + Tailwind CSS  
> **创建日期**: 2026-04-19  
> **Figma链接**: https://www.figma.com/design/o1TyPMni79X8aAbj8Zxwfu/XingChen%E5%8D%9A%E5%AE%A2%E7%B3%BB%E7%BB%9F%E8%AE%BE%E8%AE%A1

---

## 目录

- [一、设计系统规范](#一设计系统规范)
  - [1.1 色彩方案](#11-色彩方案)
  - [1.2 字体系统](#12-字体系统)
  - [1.3 圆角和阴影](#13-圆角和阴影)
  - [1.4 特殊视觉效果](#14-特殊视觉效果)
  - [1.5 响应式断点](#15-响应式断点)
- [二、核心组件库](#二核心组件库)
- [三、36个页面详细设计稿](#三36个页面详细设计稿)
- [四、交互状态规范](#四交互状态规范)
- [五、暗色模式方案](#五暗色模式方案)

---

## 一、设计系统规范

### 1.1 色彩方案

#### 品牌色（Brand Colors）

| 颜色名称 | HEX值 | RGB | 用途说明 |
|---------|-------|-----|----------|
| **Primary (主色)** | `#6366F1` | rgb(99, 102, 241) | 按钮、链接、强调元素 |
| Primary Hover | `#818CF8` | rgb(129, 140, 248) | 悬停状态 |
| Primary Subtle | `rgba(99, 102, 241, 0.15)` | - | 浅色背景 |
| Primary Glow | `rgba(99, 102, 241, 0.4)` | - | 发光效果 |

#### 辅助色（Accent Colors）

| 颜色名称 | HEX值 | 用途 |
|---------|-------|------|
| **Accent** | `#60A5FA` | 信息提示、次要操作 |
| Accent Hover | `#93C5FD` | 悬停状态 |

#### 语义色（Semantic Colors）

| 状态 | 色值 | 使用场景 |
|------|------|----------|
| ✅ Success | `#34D399` | 成功提示 |
| ⚠️ Warning | `#FBBF24` | 警告提示 |
| ❌ Error | `#F87171` | 错误提示 |

#### 亮色模式（Light Mode）

```
主背景: #ffffff
次级背景: #f9fafb
卡片背景: #ffffff
主要文字: #111827
次要文字: #374151
弱化文字: #9ca3af
边框颜色: #e5e7eb
悬停边框: #d1d5db
```

#### 暗色模式（Dark Mode）

```
主背景: #0A0A0B
次级背景: #111113
卡片背景: #16161A
主要文字: #F3F4F6
次要文字: #E5E7EB
弱化文字: #71717A
边框颜色: #27272A
悬停边框: #52525B
```

### 1.2 字体系统

```
字体栈: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif

字号层级:
- Display: 36px, Bold, 1.2 - 大型Hero标题
- H1: 30px, Bold, 1.3 - 页面主标题
- H2: 24px, SemiBold, 1.4 - 区块标题
- H3: 20px, SemiBold, 1.4 - 卡片标题
- H4: 18px, Medium, 1.5 - 小标题
- Body: 16px, Regular, 1.6 - 标准正文
- Body SM: 14px, Regular, 1.5 - 小正文
- Caption: 12px, Regular, 1.5 - 辅助说明
```

### 1.3 圆角和阴影

#### 圆角规格

```
Small (6px)   → 小按钮、输入框、标签
Medium (10px) → 中等卡片、下拉菜单
Large (14px)  → 大卡片、模态框
XLarge (18px) → 特色卡片、Hero区域
Full (9999px) → 圆形头像、药丸按钮
```

#### 阴影规格

```
Small:  0 2px 8px rgba(0,0,0,0.08)    - 小元素悬浮
Medium: 0 4px 16px rgba(0,0,0,0.12)   - 卡片默认
Large:  0 8px 32px rgba(0,0,0,0.16)   - 模态框
Glow:   0 0 20px rgba(99,102,241,0.4) - 主色调发光
```

### 1.4 特殊视觉效果

#### 玻璃拟态（Glassmorphism）

```css
.glass-card {
  background: rgba(255, 255, 255, 0.95);  /* 亮色 */
  background: rgba(22, 22, 26, 0.9);     /* 暗色 */
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
}
```

#### 渐变按钮

```css
.gradient-btn {
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: white;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3);
}
.gradient-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4);
}
```

#### 霓虹发光输入框

```css
.neon-input:focus {
  border-color: #6366F1;
  box-shadow: 
    0 0 0 3px rgba(99, 102, 241, 0.15),
    0 0 20px rgba(99, 102, 241, 0.2);
  outline: none;
}
```

### 1.5 响应式断点

```
sm:  640px   - 大屏手机
md:  768px   - 平板竖屏
lg:  1024px  - 平板横屏/笔记本
xl:  1280px  - 桌面显示器
2xl: 1536px  - 大屏显示器

容器最大宽度: max-w-screen-xl (1280px)
```

---

## 二、核心组件库

### 2.1 按钮变体

**主按钮 (Primary)**:
- 高度: 40px/44px/48px (small/default/large)
- 背景: linear-gradient(135deg, #6366F1, #8B5CF6)
- 圆角: 8px/10px
- Hover: translateY(-2px) + shadow增强

**次要按钮 (Secondary)**:
- 背景: var(--bg-elevated)
- 边框: 1px solid var(--border-color)
- Hover: 边框色变为 primary

**幽灵按钮 (Ghost)**:
- 背景: transparent
- Hover: 背景 primary-subtle + 文字 primary

### 2.2 输入框

- 高度: 42px (default), 48px (large)
- 圆角: 8px
- Focus: Neon Glow 效果 (双重发光环)
- 图标: 左侧或右侧绝对定位

### 2.3 文章卡片 (ArticleCard)

```
尺寸: 自适应宽度
圆角: 10px
图片比例: 16:9, 圆角8px
内边距: 16px

结构:
├── 图片区域 (16:9, hover scale 1.05)
├── 标题 (18px, bold, 最多2行)
├── 摘要 (14px, secondary, 最多3行)
└── 元信息行 (作者·日期·阅读量·分类)

Hover: translateY(-2px) + shadow-md
```

### 2.4 徽章/标签 (Badge)

- 形状: Pill (rounded-full)
- 内边距: 4px 10px
- 字体: 12px, medium
- 变体: Default/Success/Warning/Error (不同颜色)

### 2.5 头像 (Avatar)

```
尺寸: 24/32/40/48/64/96/120px
形状: 圆形
Hover: scale(1.1) + shadow
```

---

## 三、36个页面详细设计稿

---

### 第一批：核心页面（6个）

---

#### 📄 页面 1: 首页 (/)

**文件**: [index.vue](../front/src/views/frontend/index.vue)  
**类型**: 内容聚合页

##### 布局结构图

```
┌─────────────────────────────────────────────────────────────┐
│ HEADER (固定顶部, z-50, glass效果, h-16)                    │
│ [Logo] [导航链接] [搜索框] [主题☀️] [通知🔔] [用户👤▼]      │
├─────────────────────────────────────────────────────────────┤
│ CONTAINER (max-w-screen-xl, 居中)                           │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ CAROUSEL 轮播图                                          │ │
│ │ 尺寸: 100% × 400px | 圆角: 12px | 阴影: md             │ │
│ │ [图片1] [图片2] [图片3] + 指示器 ● ○ ○                  │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌──────────────────────────────┬───────────────────────────┐│
│ │ MAIN CONTENT (~70%)          │ SIDEBAR (~28%)            ││
│ │                              │                           ││
│ │ "最新文章" (H2, 渐变下划线)  │ ┌─────────────────────┐   ││
│ │                              │ │ UserInfoCard        │   ││
│ │ ArticleCard Grid (2列)       │ │ [头像48px] 昵称+简介 │   ││
│ │ ┌──────────┐ ┌──────────┐   │ │ 统计: 文章|关注|粉丝│   ││
│ │ │Card 1    │ │Card 2    │   │ ├─────────────────────┤   ││
│ │ │缩略图16:9│ │缩略图16:9│   │ │ HotArticles         │   ││
│ │ │标题(18px)│ │标题(18px)│   │ │ 🔥热门文章          │   ││
│ │ │摘要(14px)│ │摘要(14px)│   │ │ 1.文章A ★           │   ││
│ │ │元信息    │ │元信息    │   │ │ 2.文章B ★           │   ││
│ │ └──────────┘ └──────────┘   │ ├─────────────────────┤   ││
│ │                              │ │ CategoryList        │   ││
│ │ Pagination < 1 2 3 ... >    │ │ 📂分类列表          │   ││
│ │                              │ ├─────────────────────┤   ││
│ │                              │ │ TagCloud            │   ││
│ │                              │ │ [Vue][React][TS]... │   ││
│ │                              │ └─────────────────────┘   ││
│ │                              │ (sticky top-24)           ││
│ └──────────────────────────────┴───────────────────────────┘│
│                                                             │
│ FOOTER (glass效果, blur-xl, py-6)                            │
│ [Logo图标+名称] [链接区域] [社交图标] [版权信息]              │
└─────────────────────────────────────────────────────────────┘
```

##### 关键组件规格

**Header 导航栏**
- 高度: 64px, sticky top-0, z-50
- 背景: glass (backdrop-blur-lg, 半透明白色)
- 最大宽度: 1280px居中
- Logo: 40×40px圆形, hover scale(1.1)
- 搜索框: md以上显示, w-64~w-80, rounded-full
- 移动端: 汉堡菜单 ☰

**轮播图 Carousel**
- 尺寸: 100%宽 × 400px高
- 圆角: 12px, overflow hidden
- 指示器: 底部居中, active为胶囊形
- 箭头: 40×40px圆形, 半透明背景
- 自动播放: 5s间隔

**ArticleCard**
- 圆角: 10px, 边框 1px solid
- 图片: aspect-ratio 16/9, rounded-lg
- 标题: 18px semibold, 2行截断
- 摘要: 14px text-secondary, 3行截断
- 元信息: flex布局, 12px text-muted
- Hover: translateY(-2px) + shadow-md

**Sidebar 侧边栏**
- 宽度: ~320px, sticky top-24
- UserInfoCard: glass效果, p-5
- HotArticles: 前3名渐变色序号
- CategoryList: 图标+名称+Badge
- TagCloud: flex wrap, 不同大小颜色

---

#### 📄 页面 2: 登录页 (/login)

**文件**: [Login.vue](../front/src/views/frontend/Login.vue)  
**类型**: 认证页面

##### 布局结构图

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│          ╔══════════════════════════════════════╗            │
│          ║                                      ║            │
│          ║   渐变背景 (径向渐变, 全屏)           ║            │
│          ║   radial-gradient(紫色→蓝色→透明)     ║            │
│          ║                                      ║            │
│          ║   ┌──────────────────────────┐       ║            │
│          ║   │                          │       ║            │
│          ║   │  LOGIN CARD              │       ║            │
│          ║   │  (420×520px, 居中)       │       ║            │
│          ║   │                          │       ║            │
│          ║   │  ⭐ Logo (64×64px)       │       ║            │
│          ║   │  XingChen博客            │       ║            │
│          ║   │                          │       ║            │
│          ║   │  用户登录 (H2, 24px)     │       ║            │
│          ║   │  欢迎回来... (副标题)    │       ║            │
│          ║   │                          │       ║            │
│          ║   │  👤 用户名或邮箱         │       ║            │
│          ║   │  🔒 密码           👁   │       ║            │
│          ║   │  ┌────────┬──────────┐  │       ║            │
│          ║   │  │验证码  │  [图片]  │  │       ║            │
│          ║   │  └────────┴──────────┘  │       ║            │
│          ║   │                          │       ║            │
│          ║   │  ☑记住我    忘记密码？   │       ║            │
│          ║   │                          │       ║            │
│          ║   │  ┌────────────────────┐  │       ║            │
│          ║   │  │      登    录      │  │       ║            │
│          ║   │  │   (渐变大按钮)     │  │       ║            │
│          ║   │  └────────────────────┘  │       ║            │
│          ║   │                          │       ║            │
│          ║   │  ───── 或 ─────          │       ║            │
│          ║   │  [GitHub]  [Google]      │       ║            │
│          ║   │                          │       ║            │
│          ║   │  还没有账号？注册 →      │       ║            │
│          ║   └──────────────────────────┘       ║            │
│          ║                                      ║            │
│          ╚══════════════════════════════════════╝            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

##### 视觉规格

**登录卡片 (Glass Card)**
- 尺寸: 420×520px (自适应最小高度)
- 定位: absolute居中 (top/left 50%, translate -50%)
- 圆角: 16px
- 背景: rgba(255,255,255,0.95), backdrop-blur(20px)
- 边框: 1px solid rgba(255,255,255,0.2)
- 阴影: 0 20px 60px rgba(0,0,0,0.15) + 微弱紫光晕
- 内边距: 40px

**输入框 (Neon Input)**
- 高度: 48px, 圆角: 8px
- Focus: 边框#6366F1 + 双重发光环
  - 内环: 0 0 0 3px rgba(99,102,241,0.15)
  - 外环: 0 0 20px rgba(99,102,241,0.2)

**登录按钮 (Gradient CTA)**
- 高度: 48px, full-width, 圆角: 10px
- 渐变: 135deg, #6366F1 → #8B5CF6
- 阴影: 0 4px 16px rgba(99,102,241,0.3)
- Hover: 上移2px + 阴影增强至0 8px 24px

---

#### 📄 页面 3: 注册页 (/register)

**文件**: [Register.vue](../front/src/views/frontend/Register.vue)  
**与登录页差异**:

```
✅ 新增字段:
  - 📧 邮箱输入框
  - 确认密码输入框
  - 密码强度指示器 (弱红/中黄/强绿)

❌ 差异点:
  - 标题: "创建账号"
  - 副标题: "加入我们，开启写作之旅"
  - 底部: "已有账号？登录 →"
  - 按钮: "注册"

密码强度条:
  弱 (<6字符): ████░░░░░░░░░░░░░  #F87171
  中 (6-10): ██████████░░░░░░░  #FBBF24
  强 (>10): ███████████████████  #34D399
```

---

#### 📄 页面 4: 文章详情页 (/article/detail)

**文件**: [article-detail.vue](../front/src/views/frontend/article-detail.vue)  
**类型**: 内容展示页

##### 布局结构图

```
┌─────────────────────────────────────────────────────────────┐
│ HEADER                                                      │
├─────────────────────────────────────────────────────────────┤
│ CONTAINER                                                   │
│                                                             │
│ Breadcrumb: 🏠首页 > 技术教程 > Vue3组合式API详解             │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ARTICLE CONTAINER (白色卡片, 圆角10px, p-10)            │ │
│ │                                                         │ │
│ │ 文章标题 (H1, 30px, Bold)                               │ │
│ │                                                         │ │
│ │ 元信息: [头像40px] 作者 · 日期 · 阅读1.2k · [Vue.js]    │ │
│ │                                                         │ │
│ │ ┌──────────────────┬──────────────────────────────────┐ │ │
│ │ │ Markdown Content │ TOC Sidebar (sticky, right)     │ │ │
│ │ │ (文章正文, 16px) │                                  │ │ │
│ │ │                  │ ▸ 文章标题                       │ │ │
│ │ │ ## 第一章节       │ ▾ 1. 第一节 ← 当前高亮          │ │ │
│ │ │ 正文内容...       │ ▸ 2. 第二节                     │ │ │
│ │ │ ```代码块```     │ ▸ 3. 第三节                     │ │ │
│ │ │ > 引用块         │                                  │ │ │
│ │ │ ## 第二章节       │ (Active: 左侧3px蓝线+粗体+主色) │ │
│ │ │ 更多内容...       │                                  │ │ │
│ │ └──────────────────┴──────────────────────────────────┘ │ │
│ │                                                         │ │
│ │ 文章标签: [Vue3] [Composition API] [前端开发]           │ │
│ │                                                         │ │
│ │ 上下篇: ← 上一篇 React入门 | TypeScript技巧 →           │ │
│ │                                                         │ │
│ │ 评论区 (28条评论)                                       │ │
│ │ ┌───────────────────────────────────────────────────┐   │ │
│ │ │ [头像40px] 请输入评论...              [发布按钮]  │   │ │
│ │ │                                                  │   │ │
│ │ │ Comment 1: [头像] 用户名 · 2h前                 │   │ │
│ │ │ 这篇文章写得太棒了！        [回复] [点赞]        │   │ │
│ │ │   └ Reply: 嵌套回复                                │   │ │
│ │ │ Comment 2: ...                                    │   │ │
│ │ │ Pagination: < 1 2 3 >                             │   │ │
│ │ └───────────────────────────────────────────────────┘   │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ RIGHT SIDEBAR (~280px)                                      │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 目录 (TOC) - sticky                                     │ │
│ │ 相关文章推荐                                             │ │
│ │ 作者简介                                                 │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ FOOTER                                                      │
└─────────────────────────────────────────────────────────────┘
```

##### 关键组件

**Markdown渲染区**
- 字体: 16px, line-height 1.8
- H2: 24px semibold, border-bottom
- 代码块: bg #1e1e1e, rounded-lg, Fira Code字体
- 引用块: left-border 4px primary, bg primary-subtle
- 图片: max-w 100%, rounded-lg

**TOC目录**
- 定位: sticky, top-24
- Active: 左侧3px primary竖线 + 主色文字 + font-semibold
- 层级缩进: H2 pl-0, H3 pl-4, H4 pl-8

**评论输入框**
- textarea, rows: 4, resize vertical
- 底部工具栏: [😊表情] [@提及] + [发布按钮]

---

#### 📄 页面 5: 后台仪表盘 (/admin/dashboard)

**文件**: [Index.vue](../front/src/views/admin/Index.vue)  
**类型**: 管理后台

##### 布局结构图

```
┌────────┬──────────────────────────────────────────────────────┐
│ SIDE   │ ADMIN HEADER (深色, h-14)                           │
│ BAR    │ [Logo] Dashboard [🔔] [Admin▼] [☀️主题]             │
│ (240px)├──────────────────────────────────────────────────────┤
│        │                                                      │
│        │ CONTENT AREA (#f9fafb)                               │
│        │                                                      │
│        │ Page Header: 仪表盘 (H1) + 欢迎文字                │
│        │                                                      │
│        │ Quick Actions: [+新文章] [+新分类] [+新标签] [+新用户]│
│        │                                                      │
│        │ Stats Cards (Grid 4列)                               │
│        │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│        │ │📄 文章数 │ │💬 评论数 │ │👥 用户数 │ │👁 访问量│ │
│        │ │   128   │ │   56    │ │   34    │ │  12.5K  │ │
│        │ │  ↑12%   │ │   ↑8%   │ │  ↑15%   │ │  ↑23%   │ │
│        │ └──────────┘ └──────────┘ └──────────┘ └─────────┘ │
│        │                                                      │
│        │ Charts Area (Grid 2列)                               │
│        │ ┌─────────────────────┬───────────────────────────┐  │
│        │ │ 发布趋势 (折线图)    │ 访问来源 (Donut饼图)      │  │
│        │ │ ╱╲  ╱╲    ╱╲       │  ● 直接 40%               │  │
│        │ │╱  ╲╱  ╲╱      ╲     │  ● 搜索 35%               │  │
│        └─────────────────────┴───────────────────────────┘  │
│        │                                                      │
│        │ Recent Activity (2列Grid)                            │
│        │ 最近文章: Vue3指南 · React入门 · TS技巧              │
│        │ 最近评论: User1: 写得好! · User2: 学到了            │
│        │                                                      │
│        │ System Status                                        │
│        │ 运行时间: XX天 | 在线: XX人 | 负载: XX%             │
│        │                                                      │
└────────┴──────────────────────────────────────────────────────┘
```

##### 侧边栏菜单

```
Sidebar (fixed, w-240, h-screen, bg #0A0A0B):

Logo区 (h-16):
  [图标32px gradient] XingChen Admin

菜单项 (每项 h-11, rounded-lg):
📊 主要功能
  ├── 📈 仪表盘 ← Active: 左侧3px蓝线 + primary色
  ├── 📄 文章管理
  ├── 📁 分类管理
  ├── 🏷️ 标签管理
  └── 💬 评论管理

👥 用户与内容
  ├── 👥 用户管理
  └── 🖼️ 媒体库

⚙️ 系统
  ├── 📊 访问统计
  └── ⚙️ 站点设置

状态:
  Default: text #9CA3AF, bg transparent
  Hover: bg white/5%, text white, 左侧出现3px线
  Active: bg primary/15%, text primary, 左侧3px实线primary
```

##### 统计卡片

```
Stats Card (4个, grid-cols-4, gap-6):
  外观: white, rounded-lg, shadow-sm, p-6
  
  内部结构:
  ┌─────────────────────┐
  │ ┌──────┐   标题     │
  │ │Icon  │   文章总数  │
  │ │48px  │            │
  │ └──────┘            │
  │                      │
  │ 128 (32px, Bold)    │
  │ ↑ 12% 较上月 (green)│
  └─────────────────────┘

  图标配色:
  - 文章: bg-blue-100, icon blue-500
  - 评论: bg-green-100, icon green-500
  - 用户: bg-purple-100, icon purple-500
  - 访问: bg-yellow-100, icon yellow-500
```

---

#### 📄 页面 6: 404错误页 (/404)

**文件**: [404.vue](../front/src/views/404.vue)  
**类型**: 错误提示页

##### 布局简图

```
┌─────────────────────────────────────────────────────────────┐
│ HEADER (正常显示)                                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    (垂直居中, mt-20)                         │
│                                                             │
│              ┌───────────────────────────┐                   │
│              │                           │                   │
│              │    ┌───────────────┐       │                   │
│              │    │               │       │                   │
│              │    │ SVG 星球图标  │       │                   │
│              │    │ 200×200px     │       │                   │
│              │    │ 🌍 或 👨‍🚀    │       │                   │
│              │    └───────────────┘       │                   │
│              │                           │                   │
│              │         4 0 4             │                   │
│              │   (96px, Bold, 渐变色)     │                   │
│              │                           │                   │
│              │ 您要找的页面飞到月球去啦~   │                   │
│              │  (18px, gray-500)         │                   │
│              │                           │                   │
│              │ 可能的原因:                │                   │
│              │ • URL地址输入错误          │                   │
│              │ • 该页面已被删除            │                   │
│              │ • 您没有权限访问            │                   │
│              │                           │                   │
│              │ ┌─────────────────────┐   │                   │
│              │ │ 🏠 返回首页 (大按钮) │   │                   │
│              │ └─────────────────────┘   │                   │
│              │                           │                   │
│              └───────────────────────────┘                   │
│                                                             │
│ FOOTER                                                      │
└─────────────────────────────────────────────────────────────┘
```

**视觉规格:**
- SVG插图: 200×200px, 星球/宇航员主题, 幽默风格
- "404"文字: 96px, Bold, 渐变色(primary→accent), background-clip: text
- 副标题: 18px, gray-500
- 返回按钮: Large, Round (rounded-full), gradient背景

---

### 第二批：重要页面（6个）

---

#### 📄 页面 7: 分类列表 (/category)

**Grid 3列布局**, 每个分类卡片:
- 64×64px圆形图标 (动态颜色背景)
- 分类名 (18px semibold)
- 文章数量 Badge
- 描述文字 (2行截断)
- Hover: 上移4px + shadow-lg + 边框primary色

响应式: Desktop 3列 / Tablet 2列 / Mobile 1列

---

#### 📄 页面 8: 标签列表 (/tag)

**Tag Cloud (标签云)**:
- Flex wrap, justify-center, gap-3
- Pill形状 (rounded-full), py-2 px-4
- 字号根据热度: 16px(热门) / 14px(常用) / 13px(一般)
- 颜色从10色调色板循环选择
- Hover: scale(1.1) + 当前色背景

调色板: Indigo/Purple/Pink/Red/Orange/Amber/Emerald/Cyan/Blue

---

#### 📄 页面 9: 归档页 (/archive)

**Timeline (时间线)**:
- 左侧2px竖线 (border-color)
- 年份节点: 16px实心圆 (primary色)
- 月份节点: 12px空心圆 (2px border primary)
- 文章项: date + title, hover左侧出现primary竖线
- 可折叠/展开年月分组

---

#### 📄 页面 10: 搜索结果 (/search)

**Hero Search Box**:
- 高度56px, rounded-full (药丸形)
- Super Neon Glow focus效果 (三层发光)
- 左侧大图标24px, 右侧[清除][搜索]按钮

**结果展示**:
- 统计: "找到XX个关于'关键词'的结果"
- Filter Bar: 排序/分类/时间筛选
- ArticleCard网格 (同首页)
- EmptyState: 无结果友好提示

---

#### 📄 页面 11: 关于页 (/about)

**单列居中布局 (max-w-3xl = 768px)**:

1. **Profile区**:
   - 头像150×150px, 圆形, 3px primary边框
   - 昵称 (H2, 24px, Bold, 居中)
   - 身份Badge组 (居中)
   - 简介段落 (text-center, leading-relaxed)

2. **Skills区**: Tech Badge标签云

3. **Social Links**: GitHub/Twitter/Email/Blog图标按钮

4. **Timeline (可选)**: 经历时间线

---

#### 📄 页面 12: 个人中心 (/user)

**布局**:
- UserInfoCard (顶部): 头像100px + 昵称 + 简介 + 统计数据
- Tab导航 (6个Tab): 设置/文章/评论/收藏/分类/标签
- Tab内容区: 显示对应功能的简化版

---

### 第三批：功能页面（12个）

---

#### 📄 页面 13-24: 功能页面速查表

| # | 页面路径 | 关键特性 |
|---|---------|----------|
| 13 | `/forgot-password` | 邮箱输入 + 发送按钮 + 倒计时 + 成功状态 |
| 14 | `/verify-mfa` | 6位OTP输入框 (独立框) + 倒计时 + 盾牌图标 |
| 15 | `/user/settings/profile` | AvatarUpload + 昵称/简介/性别表单 |
| 16 | `/user/settings/account` | PasswordSettings + SecuritySettings |
| 17 | `/user/articles/create` | MDEditor (MD编辑器) + 工具栏 + 右侧设置面板 |
| 18 | `/user/articles` | 文章表格/卡片 + 操作按钮 + 状态筛选 |
| 19 | `/user/comments` | 评论列表 + 关联文章 + 回复/删除 |
| 20 | `/user/collections` | 收藏卡片网格 + 取消收藏 + 时间 |
| 21 | `/user/categories` | CRUD表格 (名称/排序/文章数) |
| 22 | `/user/tags` | CRUD表格 (名称/使用次数) |
| 23 | `/user/media` | 图片网格4列 + 上传 + 预览/删除 |
| 24 | `/notification-list` | 通知卡片 + 已读/未读 + 批量操作 |

**密码强度指示器详细规格:**

```
PasswordStrengthIndicator:
  高度: 4px, 圆角: 2px
  Level 1 - 弱 (<6字符): 进度33%, #F87171 (红)
  Level 2 - 中 (6-10字符): 进度66%, #FBBF24 (黄)
  Level 3 - 强 (>10字符+特殊): 进度100%, #34D399 (绿)
  过渡动画: 300ms ease
```

**MDEditor规格:**
- 工具栏: 加粗/斜体/标题/列表/代码/图片/链接
- 实时预览 (左右分屏或Tab切换)
- 全屏支持 + 自动保存草稿

---

### 第四批：后台管理页面（9个）

---

#### 📄 页面 25-33: 后台管理页面速查表

| # | 页面路径 | 关键组件 |
|---|---------|----------|
| 25 | `/admin/login` | 类似前台登录, 更严肃, 无第三方登录 |
| 26 | `/admin/articles` | DataTable + SearchFilterBar + 分页 + 批量操作 |
| 27 | `/admin/categories` | 树形表格 + 拖拽排序 + 图标选择 |
| 28 | `/admin/tags` | 标签云管理视图 + 使用频率统计 |
| 29 | `/admin/comments` | 审核状态表格 (待审/通过/拒绝) |
| 30 | `/admin/users` | 用户列表 + 角色管理 + 状态切换 |
| 31 | `/admin/media` | 网格/列表切换 + 存储统计 + 批量上传 |
| 32 | `/admin/settings/site` | 分组表单 (基本信息/SEO/社交/高级) |
| 33 | `/admin/analytics/page-views` | ECharts图表 + 数据指标卡 + 时间范围 |

**DataTable通用规格:**
- 列: Checkbox | 标题 | 分类 | 作者 | 状态 | 日期 | 操作
- 多选批量操作: 删除/审核/导出
- 搜索实时过滤 + 状态Dropdown + DateRange
- 排序 (点击表头) + Pagination (10/20/50 per page)
- 斑马纹 + Hover高亮 + Skeleton加载

---

### 第五批：辅助页面（3个）

---

#### 📄 页面 34-36: 辅助页面

| # | 路径 | 说明 |
|---|------|------|
| 34 | `/category/list/:id` | 分类Banner + 文章列表 + 面包屑 |
| 35 | `/tag/list/:id` | Tag Header (大号标签名) + 文章列表 |
| 36 | (备用) | 其他补充页面 |

共同特点:
- 顶部Banner: 名称 + 描述 + 文章数统计
- 文章列表: 同首页ArticleCard
- 侧边栏: 相关推荐

---

## 四、交互状态规范

### 全局交互状态矩阵

| 状态 | 视觉表现 | 应用场景 |
|------|----------|----------|
| **Default** | 基础样式 | 所有元素初始状态 |
| **Hover** | 颜色加深/上移/阴影增强 | 可交互元素鼠标悬停 |
| **Active** | 按下效果(scale 0.98) | 按钮点击瞬间 |
| **Focus** | primary边框+发光环 | 表单获得焦点 |
| **Disabled** | opacity 0.5 + not-allowed | 不可用状态 |
| **Loading** | spinner/skeleton | 数据加载中 |
| **Error** | red边框+错误提示 | 验证失败 |
| **Success** | green勾选+成功提示 | 操作完成 |

### 特殊动效时长

```
Fast:   150ms ease  - 微交互(hover/focus)
Normal: 250ms ease  - 常规过渡(layout变化)
Slow:   400ms ease  - 复杂动画(modal进出)
Shimmer: 1.5s infinite - 骨架屏流光
Glow:   2s ease-in-out infinite - 发光脉冲(可选)
Float:  3s ease-in-out infinite - 浮动动画(可选)
```

---

## 五、暗色模式方案

### 实现策略

```html
<html class="dark"> <!-- 触发暗色 -->
```

**关键变量覆盖:**
- 背景: #0A0A0B → #111113 → #16161A
- 文字: #F3F4F6 → #E5E7EB → #71717A
- 边框: #27272A → #3F3F46 → #52525B
- Glass: bg rgba(22,22,26,0.8~0.9), border rgba(255,255,255,0.08)

**保持不变的元素:**
- Gradient按钮 (始终彩色)
- Primary/Acccent品牌色
- 语义色 (Success/Warning/Error)

**过渡动画:**
```css
html { transition: background-color 300ms, color 300ms; }
```

---

## 六、代码实现映射表

| 设计元素 | 对应源文件 | 说明 |
|---------|-----------|------|
| Design Tokens | [design-tokens.css](../front/src/assets/tokens/design-tokens.css) | CSS变量定义 |
| 首页布局 | [index.vue](../front/src/views/frontend/index.vue) | Grid 4列, 3:1比例 |
| 登录表单 | [LoginForm.vue](../front/src/components/common/LoginForm.vue) | 输入框+按钮+验证码 |
| 后台仪表盘 | [Index.vue](../front/src/views/admin/Index.vue) | Stats+Charts+Activity |
| 文章详情 | [article-detail.vue](../front/src/views/frontend/article-detail.vue) | 12列栅格+TOC+Markdown |
| 404页面 | [404.vue](../front/src/views/404.vue) | SVG+文案+返回按钮 |
| Header组件 | [Header.vue](../front/src/layouts/components/Header.vue) | Sticky导航+Glass效果 |
| Footer组件 | [Footer.vue](../front/src/layouts/components/Footer.vue) | Glass Footer+Links |

---

## 附录: 设计检查清单

### 每个页面必须包含的元素

- [ ] 正确的页面标题和描述
- [ ] 符合设计系统的色彩使用
- [ ] 一致的字体层级和间距
- [ ] 所有交互状态的样式定义
- [ ] 响应式断点的适配方案
- [ ] 暗色模式的色彩适配
- [ ] Loading/Empty/Error状态处理
- [ ] 无障碍性 (焦点可见、对比度、语义标签)

### 开发还原度检查要点

1. **色彩准确性**: 使用Design Token, 不硬编码色值
2. **间距一致性**: 基于4px网格系统
3. **圆角统一**: 遵循5级圆角规范
4. **阴影层次**: 正确应用5级阴影
5. **字体排版**: 字重/行高/字号符合规范
6. **动效流畅**: 时长和缓动函数正确
7. **响应式完备**: 5个断点全部测试
8. **暗色模式**: 所有页面支持切换

---

> **文档版本**: v1.0  
> **最后更新**: 2026-04-19  
> **维护者**: XingChen Blog Design Team  
> **适用范围**: 前端开发、UI审查、产品验收

---

*本设计手稿基于XingChen博客系统实际代码提取，确保设计与实现的高度一致性。*
