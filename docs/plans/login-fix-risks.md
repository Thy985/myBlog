# 登录修复风险分析与规避方案

## 基础设施现状

| 组件 | 位置/状态 | 备注 |
|------|-----------|------|
| MySQL 8.0.12 | D:\Program Files\phpstudy_pro\Extensions\MySQL8.0.12 | 主数据库 |
| PostgreSQL + pgvector | Docker (端口 5432) | 向量搜索/AI |
| Redis | Docker (端口 6379) | 缓存 |
| MinIO | Docker (端口 9000) | 文件存储 |
| RabbitMQ | Docker (端口 5672) | 消息队列 |

## 数据库表结构现状

### 已存在的表和字段 (无需新增)

| 表 | 相关字段 | 状态 |
|----|----------|------|
| t_user | mfa_enabled, mfa_type, mfa_secret, backup_codes | ✅ 已有 |
| t_login_history | user_id, login_time, login_ip, device, browser, os, status, message | ✅ 已有 |
| (Session) | CAPTCHA_CODE | ✅ HttpSession 存储，无需表 |

### 不需要新增表或字段

本次修复 **不需要修改任何数据库表结构**，因为：
1. 验证码通过 HttpSession 存储，不落库
2. MFA 字段已在 t_user 表中
3. 登录历史表已存在
4. Token 存储在前端 (localStorage/sessionStorage)，不涉及后端

---

## 逐批风险分析

### 批次 1: 前台登录验证码对接 (P2)

**改动**: Login.vue 传递 captcha/captchaId 给 API

| 风险 | 概率 | 影响 | 规避方案 |
|------|------|------|----------|
| 验证码过期导致登录失败 | 中 | 低 | 验证码失败时自动刷新验证码，给用户友好提示 |
| 前后端验证码大小写不一致 | 低 | 低 | 后端已用 equalsIgnoreCase，无需担心 |
| Session 丢失导致验证码校验失败 | 低 | 中 | 后端已有 null 检查，返回"验证码已过期"提示 |

**回滚方案**: 移除 login() 调用中的 captcha/captchaId 参数即可

---

### 批次 2: 后台登录重写 (P3 + P4)

**改动**: admin/login.vue 加验证码 + 统一 API

| 风险 | 概率 | 影响 | 规避方案 |
|------|------|------|----------|
| 后台登录 API 调用方式改变导致登录失败 | 低 | 高 | auth.ts 的 login() 和 admin/user.ts 的 login() 调用同一个后端接口 /auth/login |
| 验证码组件引入后布局错乱 | 中 | 低 | 保持现有 Tailwind 布局，只在表单中插入 LoginCaptcha |
| 移除全局 Enter 监听后回车键失效 | 低 | 中 | 使用 @keydown.enter 在 el-form 上，确保行为一致 |

**关键验证点**:
- 确认 auth.ts 的 login() 返回格式与 admin/user.ts 的 login() 一致
- auth.ts 返回 `{ code, data: { token, userId, roles } }`
- admin/user.ts 返回 `{ code, data: { token, userId } }`
- 统一使用 auth.ts 后，admin/login.vue 需要适配新的返回格式

**回滚方案**: 恢复 admin/login.vue 原始代码

---

### 批次 3: Token 安全存储 + "记住我" (P1 + P7)

**改动**: auth.ts 条件存储 + axios.js 弹窗

| 风险 | 概率 | 影响 | 规避方案 |
|------|------|------|----------|
| **⚠️ 存储切换导致已有用户 Token 丢失** | **高** | **高** | 启动时优先读 sessionStorage，fallback 到 localStorage |
| axios.js 的 token 缓存失效 | 中 | 中 | 修改 getToken() 后需要同步更新 axios.js 的 updateCachedTokens() |
| "记住我"状态切换后 Token 丢失 | 中 | 高 | saveAuthInfo() 需要同时清理旧存储位置的 Token |
| Token 刷新时存储位置不一致 | 中 | 高 | refreshToken() 也需要根据 rememberMe 状态选择存储位置 |

**⚠️ 重点风险: 存储迁移**

当前所有用户的 Token 都在 localStorage 中。改为条件存储后：
- 如果用户未勾选"记住我"，Token 会存到 sessionStorage
- 但旧的 localStorage Token 不会自动清除
- 需要在 getToken() 中做 fallback 读取

**规避方案**:
```typescript
export function getToken(): string | undefined {
  // 优先读 sessionStorage (新逻辑)
  // fallback 到 localStorage (兼容旧逻辑)
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY) || undefined
}
```

**回滚方案**: 恢复 auth.ts 原始代码

---

### 批次 4: MFA 登录流程 (P13)

**改动**: 前端添加 MFA 输入弹窗

| 风险 | 概率 | 影响 | 规避方案 |
|------|------|------|----------|
| MFA 错误码不匹配 | 低 | 高 | 确认 ErrorCode.MFA_CODE_REQUIRED = 7003 |
| MFA 验证码格式校验 | 低 | 低 | TOTP 是 6 位数字，前端用正则校验 |
| 用户取消 MFA 输入后状态混乱 | 中 | 中 | 取消后不清除表单数据，允许重新登录 |
| MFA 与验证码同时存在时的交互 | 低 | 中 | MFA 在验证码之后校验，流程: 验证码 → 密码 → MFA |

**关键验证点**:
- ErrorCode.MFA_CODE_REQUIRED 的 code 是 7003
- 后端返回格式: `{ code: 7003, message: "请输入MFA验证码" }`
- 前端需要捕获这个特定错误码

**回滚方案**: 移除 MFA 弹窗逻辑

---

### 批次 5: 后端异常脱敏 (P17)

**改动**: GlobalExceptionHandler 修改错误消息

| 风险 | 概率 | 影响 | 规避方案 |
|------|------|------|----------|
| 前端依赖特定错误消息做判断 | 低 | 中 | 检查前端是否有 error.message.includes("xxx") 的逻辑 |
| 调试困难 | 低 | 低 | 后端日志仍保留完整异常信息 |

**验证**: 搜索前端代码确认无硬编码依赖异常消息

---

## 总体风险评估

| 批次 | 风险等级 | 数据库影响 | 回滚难度 |
|------|----------|------------|----------|
| 批次 1 | 🟢 低 | 无 | 简单 |
| 批次 2 | 🟡 中 | 无 | 简单 |
| 批次 3 | 🔴 高 | 无 | 中等 |
| 批次 4 | 🟡 中 | 无 | 简单 |
| 批次 5 | 🟢 低 | 无 | 简单 |

## 执行建议

1. **批次 3 (Token 存储) 风险最高**，建议最后执行或单独执行
2. 每批修改后需要测试:
   - 前台登录 (含验证码)
   - 后台登录 (含验证码)
   - Token 刷新
   - 退出登录
3. 建议在修改前备份关键文件:
   - front/src/composables/auth.ts
   - front/src/axios.js
   - front/src/views/admin/login.vue

## 不需要的操作

- ❌ 不需要修改 MySQL 表结构
- ❌ 不需要修改 Docker 容器配置
- ❌ 不需要新增数据库表
- ❌ 不需要修改后端 Service 层 (MFA 逻辑已存在)
