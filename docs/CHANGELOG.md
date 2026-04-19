# 📋 CHANGELOG - 变更日志

所有重要的项目更新都将记录在此文件中。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [0.1.0] - 2026-04-19

### 🎯 重大更新

#### AI 智能体重构
- ✨ 新增 Tool 系统（ArticleQueryTool, ArticlePublishTool, ArticleUpdateTool, ArticleDeleteTool）
- ✨ 新增 CategoryTool 和 TagTool 分类标签管理工具
- ✨ 新增 ToolRegistry 工具注册中心
- ✨ 新增 SkillExecutor 技能执行器
- ✨ 优化 HybridIntentClassifier 混合意图分类器
- ✨ 新增 MCP 客户端服务（McpClientService）
- ✨ 增强 EmbeddingIntentClassifier 向量意图分类

#### 图片服务
- ✨ 新增 ImageService 图片上传服务
- ✨ 新增 ImageServiceImpl 图片服务实现
- ✨ 新增 MinioConfig MinIO 配置

#### 内容管理
- ✨ 优化 ArticleServiceImpl 文章服务
- ✨ 优化 CategoryServiceImpl 分类服务
- ✨ 优化 TagServiceImpl 标签服务
- ✨ 新增 AnalyticsServiceImpl 分析服务实现

#### 基础设施
- ✨ 新增 ApiKeyEncryptionInitializer API Key 加密初始化器
- ✨ 优化 RedisCacheConfig 缓存配置
- ✨ 优化 FileServiceImpl 文件处理

### 🐛 Bug 修复
- 暂无记录

### 📚 文档更新
- ✏️ 新增页面跳转代码分析报告
- ✏️ 更新用户使用指南文档
- ✏️ 新增页面跳转指南
- ✏️ 更新个人博客系统整合需求文档

---

## [0.0.9] - 2026-04-12

### 🎯 更新

#### AI 智能体
- ✨ 智能体博客发布功能增强 - AI生成真实内容
- ✨ API Key 加密存储和熔断保护功能
- ✨ 多用户 API Key 配置和模型隔离
- ✨ 智能体系统架构重构

#### UI/UX
- ✨ 统一UI/UX - 加载状态、空状态、样式系统
- ✨ 统一空状态、骨架屏、错误处理、搜索增强
- ✨ 重构前端大型组件为可复用模块

#### 安全
- 🛡️ 修复头像上传安全漏洞
- 🛡️ 添加 E2E 测试框架

### 🐛 修复
- 修复头像上传安全漏洞和缓存问题

---

## [0.0.8] - 2026-03-24

### 🎯 更新

#### 功能增强
- ✨ 统一 UI/UX 组件系统
- ✨ 新增 E2E 测试框架（Playwright + 验证码控制器）
- ✨ 前端组件模块化重构

### 📚 文档
- ✏️ 更新前端子模块
- ✏️ 添加 COMPONENT_STYLE_GUIDE.md 组件样式指南

---

## [0.0.7] - 2026-03-15

### 🎯 更新

#### 基础功能
- ✨ 清理调试文件，优化 .gitignore
- ✨ 移除部署相关文件

---

## 版本历史说明

| 版本 | 说明 |
|------|------|
| 0.1.0 | AI 智能体全面重构，Tool 系统上线 |
| 0.0.9 | AI 博客功能增强，安全修复 |
| 0.0.8 | UI/UX 统一，E2E 测试框架 |
| 0.0.7 | 基础清理 |

---

**格式说明：**

- ✨ 新功能
- 🐛 Bug 修复
- 🔧 功能变更
- 📚 文档更新
- 🚀 性能优化
- 🛡️ 安全修复
- 💄 UI/样式更新
