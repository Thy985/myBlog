# 字段命名规范

## 1. 概述

本文档定义了前端与后端交互时的字段命名映射规范，确保数据在组件间的一致性。

## 2. 字段映射表

### 2.1 文章相关字段

| 后端字段 | 前端字段 | 说明 |
|---------|---------|------|
| `readNum` | `readCount` | 阅读数量 |
| `viewCount` | `readCount` | 浏览数量（统一使用 readCount） |
| `summary` | `description` | 文章摘要（统一使用 description） |
| `description` | `description` | 文章描述 |
| `createdTime` | `createdAt` | 创建时间（统一使用 createdAt） |
| `updatedTime` | `updatedAt` | 更新时间（统一使用 updatedAt） |

### 2.2 统一字段命名

- **阅读数量**: 统一使用 `readCount`
- **文章摘要**: 统一使用 `description`
- **创建时间**: 统一使用 `createdAt`
- **更新时间**: 统一使用 `updatedAt`

## 3. 数据转换层

在 API 响应处理时，应在数据获取处进行字段映射，而不是在每个组件中单独处理。

### 3.1 推荐做法

```javascript
// 在 API 层统一转换
function transformArticle(article) {
  return {
    id: article.id,
    title: article.title,
    description: article.description || article.summary,
    readCount: article.readCount || article.readNum || article.viewCount || 0,
    createdAt: article.createdAt || article.createdTime,
    updatedAt: article.updatedAt || article.updatedTime,
    // ... 其他字段
  }
}
```

### 3.2 不推荐做法

```javascript
// 不推荐：在不同组件中使用不同字段名
{{ article.summary }}           // ❌
{{ article.description }}      // ❌
{{ article.readNum }}          // ❌
{{ article.viewCount }}        // ❌
```

## 4. 组件内字段使用规范

组件内部应使用统一的字段名，如需从 props 或 API 获取数据，应在组件入口处进行映射转换。

## 5. 现有组件字段使用情况

| 组件 | 使用的字段 | 统一后应使用 |
|-----|----------|------------|
| ArticleCard.vue | `summary`, `readNum` | `description`, `readCount` |
| Carousel.vue | `description` | `description` ✓ |
| HotArticles.vue | `readNum`, `createdAt` | `readCount`, `createdAt` ✓ |
| DashboardActivity.vue | `viewCount` | `readCount` |
