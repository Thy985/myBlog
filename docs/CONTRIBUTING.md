# 🤝 贡献指南

感谢您对 BlogGrowth AI 的兴趣！我们欢迎各种形式的贡献，包括但不限于代码、文档、Bug 报告和功能建议。

## 📋 目录

- [行为准则](#行为准则)
- [开始贡献](#开始贡献)
- [开发环境](#开发环境)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)
- [Issue 指南](#issue-指南)
- [功能建议](#功能建议)

---

## 行为准则

请尊重所有参与者，保持友好和专业的交流氛围。我们坚持：
- 使用包容性语言
- 尊重不同的观点和经验
- 建设性地接受批评
- 关注对社区最有利的事情

---

## 开始贡献

### 1. Fork 项目

点击 GitHub 页面右上角的 Fork 按钮，或执行：

```bash
# 克隆你的 Fork
git clone https://github.com/YOUR_USERNAME/myBlog.git
cd myBlog

# 添加上游仓库
git remote add upstream https://github.com/Thy985/myBlog.git

# 验证
git remote -v
# origin    https://github.com/YOUR_USERNAME/myBlog.git (fetch)
# origin    https://github.com/YOUR_USERNAME/myBlog.git (push)
# upstream  https://github.com/Thy985/myBlog.git (fetch)
# upstream  https://github.com/Thy985/myBlog.git (push)
```

### 2. 创建分支

```bash
# 确保基于最新 main 分支
git fetch upstream
git checkout main
git rebase upstream/main

# 创建功能分支
git checkout -b feature/your-feature-name
# 或修复 Bug
git checkout -b fix/issue-number-brief-description
```

### 3. 设置开发环境

参见 [DEPLOYMENT.md](DEPLOYMENT.md) 的「开发环境配置」章节。

---

## 代码规范

### Java (后端)

遵循 Google Java Style Guide，使用以下配置：

**IDE 配置：**
- IntelliJ IDEA: 安装 Google Java Format 插件
- VS Code: 安装 java-formatter 扩展

**格式化：**
```bash
# 格式化代码
mvn com.coveo:fmt-maven-plugin:format
```

**检查：**
```bash
mvn checkstyle:check
```

**关键规范：**
- 类名使用 UpperCamelCase：`UserService`, `ArticleController`
- 方法名使用 lowerCamelCase：`getUserById`, `createArticle`
- 常量使用 UPPER_SNAKE_CASE：`MAX_RETRY_COUNT`
- 4 空格缩进，线条长度不超过 100 字符
- 所有 public 方法必须有 Javadoc

### TypeScript (前端)

遵循 ESLint + Prettier 配置：

```bash
cd front
pnpm lint      # 检查
pnpm format    # 格式化
```

**关键规范：**
- 组件名使用 PascalCase：`ArticleCard.vue`, `UserProfile.vue`
- 工具函数使用 camelCase：`formatDate.ts`, `calculateReadTime.ts`
- 2 空格缩进
- 禁用 `any`，优先使用 `unknown` 或具体类型

### Commit Message

遵循 Conventional Commits：

```
<type>(<scope>): <subject>

feat(ai): add intent classifier for user query analysis
fix(backend): resolve article publish race condition
docs: update deployment guide for Docker Compose
```

**Type 类型：**
| Type | 说明 |
|------|------|
| feat | 新功能 |
| fix | Bug 修复 |
| docs | 文档更新 |
| style | 代码格式（不影响功能） |
| refactor | 重构 |
| perf | 性能优化 |
| test | 测试相关 |
| chore | 构建/工具变动 |

---

## 提交规范

### 提交前检查

```bash
# 后端
cd backend
mvn clean compile        # 确保编译通过
mvn test                 # 运行测试
mvn fmt:check            # 检查格式

# 前端
cd front
pnpm build               # 确保构建成功
pnpm type-check          # 类型检查
pnpm lint                # ESLint 检查
```

### Commit 示例

**✅ 正确示例：**
```
feat(ai): add opportunity discovery agent

- analyze user search queries
- identify content gaps
- generate task recommendations

Closes #123
```

**❌ 错误示例：**
```
fix stuff
update code
WIP
```

---

## Pull Request 流程

### 1. 保持同步

```bash
git fetch upstream
git rebase upstream/main
# 解决可能的冲突
```

### 2. 推送分支

```bash
git push origin feature/your-feature-name
```

### 3. 创建 PR

在 GitHub 上创建 Pull Request，描述：
- **标题**: 清晰描述改动
- **描述**: 说明动机和实现方式
- **链接**: 关联相关 Issue

PR 模板：
```markdown
## 🎯 实现的功能

描述你实现的功能或修复的问题。

## 🔧 改动说明

- 具体改动了什么
- 为什么这样改动

## 📸 截图（如有 UI 改动）

## ✅ 检查清单

- [ ] 代码编译/构建通过
- [ ] 测试通过
- [ ] 文档已更新
- [ ] 无新增警告
```

### 4. Review 流程

- 至少 1 个 Reviewer 批准方可合并
- 根据反馈及时修改
- 确保 CI/CD 通过

### 5. 合并

合并方式选择：
- **Squash and merge**: 推荐，保持历史整洁
- **Merge commit**: 用于多 commit PR

---

## Issue 指南

### 创建 Issue 前

- [ ] 确认问题是否是新版本中
- [ ] 搜索是否已有相同 Issue
- [ ] 如果是 Bug，提供复现步骤

### Issue 模板

**Bug 报告：**
```markdown
## 🐛 Bug 描述
清晰描述问题

## 🔄 复现步骤
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

## ✅ 预期行为
描述预期结果

## 📸 截图
如有需要

## 📋 环境信息
- OS: [e.g. macOS 14.0]
- Browser: [e.g. Chrome 120]
- Version: [e.g. 1.2.0]
```

**功能请求：**
```markdown
## 💡 功能描述
清晰描述你希望实现的功能

## 🎯 使用场景
描述这个功能的使用场景和价值

## 🔧 建议的实现方式
如果有建议的实现思路

## 📝 其他说明
其他补充信息
```

---

## 功能建议

欢迎提出功能建议！请使用「功能请求」模板，并说明：

1. **解决的问题**：这个功能解决什么问题？
2. **使用场景**：具体的使用场景是什么？
3. **实现建议**：你有初步的实现思路吗？

---

## 🙏 感谢

感谢所有贡献者！

<!-- 贡献者列表将通过 GitHub API 自动生成 -->
