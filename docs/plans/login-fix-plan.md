# 登录体验修复计划

> **目标**: 修复分析报告中发现的 10 个问题，按优先级分批执行
> **架构**: Vue 3 + Element Plus 前端 / Spring Boot 3.4 + Sa-Token 后端

---

## 代码现状分析

通过逐行阅读代码，我发现有些问题已经被部分修复：

| 问题 | 现状 | 需要改动 |
|------|------|----------|
| P2 验证码未校验 | 后端 AuthController 已有 verifyCaptcha()，前端 LoginCaptcha 已 emit captchaId，但 **Login.vue 未传 captcha** | 只改 Login.vue |
| P3 后台无验证码 | admin/login.vue 完全没有验证码 | 改 admin/login.vue |
| P13 MFA 未接入 | 后端 UserServiceImpl.login() **已有 MFA 逻辑**，但前端无 MFA 输入弹窗 | 改前端登录流程 |
| P1 Token 存 localStorage | composables/auth.ts 无条件用 localStorage | 改 auth.ts |
| P9 全局 Enter 监听 | admin/login.vue 用 document.addEventListener | 改 admin/login.vue |
| P19 autocomplete | LoginForm.vue **已修复**，admin/login.vue 仍是 off | 改 admin/login.vue |
| P7 "记住我"不生效 | saveAuthInfo 无条件用 localStorage | 改 auth.ts |
| P4 API 不统一 | admin/login.vue 用 admin/user.ts 的 login() | 改 admin/login.vue |
| P17 异常信息泄露 | GlobalExceptionHandler 的 SaTokenException 暴露 e.getMessage() | 改后端 |
| P11 Token 刷新失败硬跳转 | axios.js 用 setTimeout 1s 跳转 | 改 axios.js |

---

## 批次 1: 前端 — 前台登录验证码对接 (P2)

**问题**: Login.vue 的 handleLoginSubmit 只传 username 和 password，未传 captcha/captchaId
**改动范围**: `front/src/views/frontend/Login.vue`

### 改动 1: Login.vue — 传递验证码到 API

在 `handleLoginSubmit` 中，将 `formData.captcha` 和 `formData.captchaId` 传给 login API：

```typescript
// 当前代码 (行 42-45):
const res = await login({
  username: formData.account,
  password: formData.password
})

// 改为:
const res = await login({
  username: formData.account,
  password: formData.password,
  captcha: formData.captcha,
  captchaId: formData.captchaId
})
```

同时需要在登录失败时刷新验证码：

```typescript
// 在 catch 块中添加:
catch (error) {
  handleApiError(error)
  logger.error('登录失败:', error)
  // 刷新验证码
  loginFormRef.value?.resetCaptcha?.()
}
```

需要在 LoginForm.vue 中暴露 resetCaptcha 方法：

```typescript
// LoginForm.vue 的 defineExpose 中添加:
defineExpose({
  validate,
  resetForm,
  formData,
  resetCaptcha: () => {
    // 触发验证码刷新
  }
})
```

---

## 批次 2: 前端 — 后台登录接入验证码 + 统一 API (P3 + P4)

**问题**: admin/login.vue 无验证码，且用独立的 login(username, password) API
**改动范围**: `front/src/views/admin/login.vue`

### 改动 2: admin/login.vue — 完整重写

1. 导入改为使用 `@/api/auth` 的 `login` 函数
2. 添加 LoginCaptcha 组件
3. 移除全局 Enter 监听，改用 `@keydown.enter`
4. 修复 autocomplete="off"
5. 表单数据增加 captcha/captchaId 字段

```vue
<template>
  <div>
    <div class="grid grid-cols-6 h-screen bg-white">
      <!-- 左边栏 (不变) -->
      <div class="col-span-6 md:col-span-3 sm:col-span-6">
        <div class="login-container-left">
          <div class="login-brand">
            <h2>星辰博客登录</h2>
            <p>走向星辰大海，致力未来远征</p>
          </div>
          <img src="@/assets/头像.jpg" class="login-image" loading="lazy" alt="登录页面插图">
        </div>
      </div>
      <!-- 右边栏 -->
      <div class="col-span-6 px-3 md:col-span-3 sm:col-span-6">
        <div class="login-container-right flex justify-center items-center flex-col">
          <h2 class="font-bold text-3xl text-gray-800 mt-5">欢迎回来</h2>
          <div class="flex items-center justify-center my-5 text-gray-400 space-x-2">
            <span class="h-[1px] w-16 bg-gray-200"></span>
            <span>账号密码登录</span>
            <span class="h-[1px] w-16 bg-gray-200"></span>
          </div>
          <div>
            <el-form
              ref="formRef"
              :rules="rules"
              :model="form"
              class="w-[300px]"
              @keydown.enter="onSubmit"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="form.username"
                  :prefix-icon="User"
                  placeholder="请输入用户名"
                  size="large"
                  clearable
                  autocomplete="username"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="form.password"
                  type="password"
                  autocomplete="current-password"
                  :prefix-icon="Lock"
                  placeholder="请输入密码"
                  show-password
                  size="large"
                  clearable
                />
              </el-form-item>
              <el-form-item prop="captcha">
                <LoginCaptcha
                  v-model="form.captcha"
                  @update:captchaId="form.captchaId = $event"
                  @submit="onSubmit"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  round
                  type="primary"
                  :loading="loading"
                  class="w-[300px] login-btn mt-4"
                  size="large"
                  @click="onSubmit"
                >
                  登 录
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { login } from '@/api/auth'  // 改用统一的 auth API
import { showMessage } from '@/utils'
import { useRouter } from 'vue-router'
import { setToken, getRedirectUrl, clearRedirectUrl } from '@/composables/auth'
import { useAuthStore } from '@/stores/auth'
import { User, Lock } from '@element-plus/icons-vue'
import { API_STATUS } from '@/composables/api'
import logger from '@/utils/logger'
import LoginCaptcha from '@/components/common/LoginCaptcha.vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaId: ''
})

const rules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  captcha: [{ required: true, message: '验证码不能为空', trigger: 'blur' }]
}

const onSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return false
    loading.value = true
    try {
      const res = await login({
        username: form.username,
        password: form.password,
        captcha: form.captcha,
        captchaId: form.captchaId
      })
      if (res.code === API_STATUS.SUCCESS) {
        showMessage('登录成功', 'success')
        setToken(res.data.token)
        authStore.setUser(res.data.user || {})
        try {
          await authStore.getAdminInfo()
        } catch (error) {
          logger.error('获取用户信息失败:', error)
        }
        const redirectUrl = getRedirectUrl()
        if (redirectUrl !== '/login' && redirectUrl !== '/admin/login') {
          clearRedirectUrl()
          router.push(redirectUrl)
        } else {
          router.push('/admin')
        }
      } else {
        showMessage(res.message, 'error')
      }
    } catch (error) {
      if (error.response) {
        switch (error.response.status) {
          case 401: showMessage('账号或密码错误，请重新输入', 'error'); break
          case 403: showMessage('账号已被禁用，请联系管理员', 'error'); break
          case 429: showMessage('登录过于频繁，请稍后重试', 'error'); break
          default: showMessage('登录失败，请稍后重试', 'error')
        }
      } else if (error.request) {
        showMessage('网络连接失败，请检查网络设置', 'error')
      } else {
        showMessage('登录失败，请稍后重试', 'error')
      }
      logger.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}
// 移除 onMounted/onBeforeUnmount 中的 document.addEventListener
</script>
```

---

## 批次 3: 前端 — Token 安全存储 + "记住我" (P1 + P7)

**问题**: Token 无条件存 localStorage，"记住我"名不副实
**改动范围**: `front/src/composables/auth.ts`, `front/src/axios.js`

### 改动 3a: auth.ts — 条件存储 Token

```typescript
// 改 setToken 为条件存储:
export function setToken(token: string, rememberMe: boolean = false): void {
  if (rememberMe) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
    localStorage.removeItem(TOKEN_KEY)  // 清除旧的
  }
}

// 改 getToken 优先读 sessionStorage:
export function getToken(): string | undefined {
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY) || undefined
}

// 改 saveAuthInfo 传递 rememberMe:
export function saveAuthInfo(token: string, refreshToken: string, rememberMe: boolean): void {
  setToken(token, rememberMe)
  if (rememberMe) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  } else {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }
  setRememberMe(rememberMe)
}

// 改 getRefreshToken:
export function getRefreshToken(): string | undefined {
  return sessionStorage.getItem(REFRESH_TOKEN_KEY) || localStorage.getItem(REFRESH_TOKEN_KEY) || undefined
}

// 改 clearAuthInfo 同时清理两个存储:
export function clearAuthInfo(): void {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(REMEMBER_ME_KEY)
  removeCsrfToken()
}
```

### 改动 3b: axios.js — Token 刷新失败改用 ElMessageBox

```typescript
// 替换 setTimeout 跳转为弹窗确认:
// 当前代码 (行 91-96):
if (!window.location.pathname.includes('/login')) {
  showMessage('登录已过期，请重新登录', 'warning')
  setTimeout(() => {
    router.push('/login')
  }, 1000)
}

// 改为:
if (!window.location.pathname.includes('/login')) {
  import('element-plus').then(({ ElMessageBox }) => {
    ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login')
    }).catch(() => {
      // 用户取消，不跳转
    })
  })
}
```

---

## 批次 4: 前端 — MFA 登录流程 (P13)

**问题**: 后端已有 MFA 校验，但前端登录时无 MFA 输入弹窗
**改动范围**: `front/src/views/frontend/Login.vue`, `front/src/views/admin/login.vue`

当后端返回 MFA_CODE_REQUIRED 错误时，前端需要弹出 MFA 输入框。

### 改动 4: Login.vue — 处理 MFA 响应

在 handleLoginSubmit 的 catch 中添加 MFA 处理：

```typescript
// 在 catch 块中:
catch (error) {
  if (error.response?.data?.code === 10005) {  // MFA_CODE_REQUIRED
    // 弹出 MFA 输入框
    showMfaDialog(error.response.data)
    return
  }
  handleApiError(error)
}
```

添加 MFA 弹窗逻辑（使用 ElMessageBox.prompt）：

```typescript
import { ElMessageBox } from 'element-plus'

const showMfaDialog = async (responseData) => {
  try {
    const { value: mfaCode } = await ElMessageBox.prompt(
      '请输入两步验证码',
      '两步验证',
      {
        confirmButtonText: '验证',
        cancelButtonText: '取消',
        inputPlaceholder: '6位验证码',
        inputPattern: /^\d{6}$/,
        inputErrorMessage: '请输入6位数字验证码'
      }
    )
    // 重新登录，带上 mfaCode
    const res = await login({
      username: formData.account,
      password: formData.password,
      captcha: formData.captcha,
      captchaId: formData.captchaId,
      mfaCode: mfaCode
    })
    // ... 处理登录成功逻辑
  } catch {
    // 用户取消
  }
}
```

---

## 批次 5: 后端 — 异常信息脱敏 (P17)

**问题**: SaTokenException 处理器暴露了 e.getMessage()
**改动范围**: `backend/.../exception/GlobalExceptionHandler.java`

### 改动 5: GlobalExceptionHandler.java

```java
// 当前代码 (行 59-62):
@ExceptionHandler(SaTokenException.class)
public Result<Void> handleSecurityException(SaTokenException e) {
    log.warn("安全异常: {}", e.getMessage());
    return Result.fail(403, "请求不安全: " + e.getMessage());  // 暴露了内部信息
}

// 改为:
@ExceptionHandler(SaTokenException.class)
public Result<Void> handleSecurityException(SaTokenException e) {
    log.warn("安全异常: {}", e.getMessage());
    return Result.fail(403, "请求不安全，请重新登录");
}
```

---

## 执行顺序

1. ✅ 批次 1: 前台登录验证码对接 (最小改动，只改 Login.vue 一行)
2. ✅ 批次 2: 后台登录重写 (验证码 + 统一 API + Enter + autocomplete)
3. ✅ 批次 3: Token 安存 + "记住我" + Token 刷新体验
4. ✅ 批次 4: MFA 登录流程
5. ✅ 批次 5: 后端异常脱敏

## 验证方式

每批修改后需要验证:
1. 前台登录流程正常（含验证码）
2. 后台登录流程正常（含验证码）
3. "记住我"功能正常
4. Token 刷新/过期处理正常
5. MFA 登录流程正常（如果用户启用了 MFA）
6. 后端编译通过
