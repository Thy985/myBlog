# 文章阅读量实时更新 - Product Requirement Document

## Overview
- **Summary**: 修复文章阅读量显示为固定数字的问题，确保每次用户访问文章详情页时，阅读量都能正确增加并实时显示。
- **Purpose**: 解决当前文章阅读量不会随着实际浏览次数增加而增加的问题，提升用户体验和数据准确性。
- **Target Users**: 博客系统的所有用户

## Goals
- 确保每次访问文章详情页时，阅读量正确递增
- 确保前端显示的阅读量与数据库中的实际值一致
- 解决缓存导致的阅读量不更新问题
- 保持代码结构清晰和性能优化

## Non-Goals (Out of Scope)
- 不修改用户点赞、收藏等其他统计数据
- 不改变文章列表页面的显示逻辑
- 不重构整个缓存系统架构

## Background & Context
当前问题分析：
1. 在 `ArticleServiceImpl.getArticleById` 方法中，先查询 Article 对象
2. 然后调用 `articleMapper.incrementReadNum(id)` 增加数据库中的阅读量
3. 但返回的 ArticleVO 使用的是最初查询的旧 Article 对象，导致阅读量显示为旧值
4. 同时，`@Cacheable` 注解会缓存包含旧阅读量的数据，进一步加剧问题

## Functional Requirements
- **FR-1**: 每次调用 `getArticleById` 时，返回的阅读量应该是递增后的最新值
- **FR-2**: 更新阅读量后，相关缓存应该被清除或更新
- **FR-3**: 前端显示的阅读量应该与后端返回的最新值一致

## Non-Functional Requirements
- **NFR-1**: 阅读量更新操作的响应时间应该保持在合理范围内（<100ms）
- **NFR-2**: 不应该因为修复此问题而影响其他功能的正常运行
- **NFR-3**: 代码修改应该遵循现有项目的代码规范和风格

## Constraints
- **Technical**: 使用现有的 Spring Boot + MyBatis-Flex 技术栈
- **Business**: 保持现有的缓存策略，只针对阅读量问题进行调整
- **Dependencies**: 依赖现有的 Redis 缓存和数据库连接

## Assumptions
- 假设数据库中的阅读量字段能够正确递增
- 假设现有的缓存机制可以通过 `@CacheEvict` 注解正确清除
- 假设前端能够正确处理后端返回的更新后的数据

## Acceptance Criteria

### AC-1: 阅读量正确递增
- **Given**: 用户访问文章详情页
- **When**: 后端接收到获取文章详情的请求
- **Then**: 数据库中的该文章阅读量增加1，返回给前端的阅读量也是增加后的值
- **Verification**: `programmatic`
- **Notes**: 可以通过多次刷新页面并对比数据库值来验证

### AC-2: 缓存正确处理
- **Given**: 文章详情已经被缓存
- **When**: 同一文章再次被访问
- **Then**: 缓存中应该包含最新的阅读量数据，或者缓存被清除后重新获取
- **Verification**: `programmatic`
- **Notes**: 验证缓存清除或更新机制

### AC-3: 前端显示正确
- **Given**: 后端返回了更新后的阅读量数据
- **When**: 前端接收到响应并渲染页面
- **Then**: 页面上显示的阅读量数字应该与后端返回的值一致
- **Verification**: `human-judgment`
- **Notes**: 通过浏览器开发者工具查看网络请求和页面显示来验证

## Open Questions
- 无
