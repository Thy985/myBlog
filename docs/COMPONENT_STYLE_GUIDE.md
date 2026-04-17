# 组件风格规范

## 1. 概述

本文档定义了项目中的组件风格规范，确保 UI 的一致性。

## 2. 按钮系统

项目使用统一的按钮系统，定义在 `main.css` 中。

### 2.1 按钮类型

| 按钮类型 | 类名 | 说明 |
|---------|------|------|
| 主要按钮 | `.btn.btn-primary` | 用于主要操作 |
| 次要按钮 | `.btn.btn-secondary` | 用于次要操作 |
| 轮廓按钮 | `.btn.btn-outline` | 用于不那么重要的操作 |
| 文本按钮 | `.btn.btn-text` | 用于辅助操作 |
| 强调按钮 | `.btn.btn-accent` | 用于警告或特殊操作 |

### 2.2 使用规范

```html
<!-- 推荐：使用统一的按钮类 -->
<button class="btn btn-primary px-4 py-2">登录</button>
<button class="btn btn-outline">取消</button>

<!-- 不推荐：混用不同风格 -->
<button class="tech-badge">...</button>
<el-button type="primary">...</el-button> <!-- 在非必要情况下 -->
```

## 3. 样式变量系统

项目使用 CSS 变量系统，定义在 `main.css` 中。

### 3.1 主色调

```css
--primary-color: #3b82f6;     /* 主色 */
--primary-hover: #2563eb;    /* 悬停色 */
--primary-light: #dbeafe;    /* 浅色背景 */
--primary-dark: #1d4ed8;     /* 深色强调 */
```

### 3.2 文本色

```css
--text-primary: #111827;     /* 主要文本 */
--text-secondary: #374151;    /* 次要文本 */
--text-tertiary: #6b7280;    /* 辅助文本 */
--text-muted: #9ca3af;       /* 静音文本 */
```

### 3.3 背景色

```css
--background-primary: #ffffff;    /* 主背景 */
--background-secondary: #f9fafb; /* 次要背景 */
--background-tertiary: #f3f4f6;   /* 第三背景 */
--background-light: var(--background-primary);
```

### 3.4 边框色

```css
--border-color: #e5e7eb;      /* 主边框 */
--border-hover: #d1d5db;      /* 悬停边框 */
--border-active: #9ca3af;     /* 激活边框 */
```

## 4. 组件分类

### 4.1 通用组件 (`components/common/`)

- ArticleCard.vue - 文章卡片
- CommentList.vue - 评论列表
- CommentItem.vue - 评论项
- HotArticles.vue - 热门文章
- RelatedArticles.vue - 相关文章
- UserInfoCard.vue - 用户信息卡片

### 4.2 UI 组件 (`components/ui/`)

- EmptyState.vue - 统一空状态组件
- Carousel.vue - 轮播组件
- Pagination.vue - 分页组件
- CountTo.vue - 数字动画组件
- ThemeSwitcher.vue - 主题切换
- ResponsiveImage.vue - 响应式图片

### 4.3 布局组件 (`components/layout/`)

- Header.vue - 页头
- Footer.vue - 页脚
- SearchBar.vue - 搜索框
- UserMenu.vue - 用户菜单
- NavLinks.vue - 导航链接
- MobileMenu.vue - 移动端菜单

### 4.4 科技风格组件 (`components/tech/`)

- TechBadge.vue - 科技风格标签
- TechCard.vue - 科技风格卡片

## 5. 空状态设计

统一使用 `EmptyState.vue` 组件：

```vue
<EmptyState
  icon="document"
  title="暂无数据"
  description="这里还没有内容哦~"
  action-text="去创建"
  :show-action="true"
  :show-tip="true"
  tip="试试其他分类"
  @action="handleAction"
/>
```

支持的图标类型：`document`, `chat`, `folder`, `search`, `bell`, `user`, `picture`, `link`, `star`

## 6. 暗色模式支持

组件应使用 CSS 变量而非硬编码颜色：

```css
/* 推荐 */
color: var(--text-primary);
background-color: var(--background-light);

/* 不推荐 */
color: #1f2937;
background-color: white;
```

## 7. 字段命名规范

参见 [FIELD_MAPPING.md](./FIELD_MAPPING.md)
