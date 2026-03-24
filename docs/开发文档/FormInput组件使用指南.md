# FormInput 组件使用指南

## 简介

`FormInput` 是一个增强的表单输入组件，提供实时验证、视觉反馈和无障碍访问支持。

## 特性

- ✅ 实时表单验证
- ✅ 成功/错误状态视觉反馈
- ✅ 自定义验证规则
- ✅ 无障碍访问支持 (ARIA)
- ✅ 键盘导航友好
- ✅ 支持深色模式

## 基础用法

```vue
<template>
  <FormInput
    v-model="username"
    label="用户名"
    placeholder="请输入用户名"
    required
  />
</template>

<script setup>
import { ref } from 'vue'
import FormInput from '@/components/FormInput.vue'

const username = ref('')
</script>
```

## Props 属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modelValue | String/Number | '' | 输入框的值 (v-model) |
| label | String | '' | 输入框标签 |
| type | String | 'text' | 输入框类型 (text/email/password等) |
| placeholder | String | '' | 占位符文本 |
| disabled | Boolean | false | 是否禁用 |
| required | Boolean | false | 是否必填 |
| rules | Array | [] | 验证规则数组 |
| helpText | String | '' | 帮助文本 |
| successMessage | String | '' | 验证成功提示 |

## 验证规则

验证规则是一个函数数组，每个函数接收输入值，返回 `true` 或错误消息字符串。

### 内置验证示例

```vue
<script setup>
// 邮箱验证
const emailRules = [
  (v) => /.+@.+\..+/.test(v) || '请输入有效的邮箱地址'
]

// 密码强度验证
const passwordRules = [
  (v) => v.length >= 8 || '密码至少8个字符',
  (v) => /[A-Z]/.test(v) || '密码必须包含大写字母',
  (v) => /[0-9]/.test(v) || '密码必须包含数字'
]

// 手机号验证
const phoneRules = [
  (v) => /^1[3-9]\d{9}$/.test(v) || '请输入有效的手机号'
]

// 自定义验证
const usernameRules = [
  (v) => v.length >= 3 || '用户名至少3个字符',
  (v) => v.length <= 20 || '用户名最多20个字符',
  (v) => /^[a-zA-Z0-9_]+$/.test(v) || '用户名只能包含字母、数字和下划线'
]
</script>
```

## 完整示例

### 注册表单

```vue
<template>
  <form @submit.prevent="handleSubmit" class="max-w-md mx-auto p-6">
    <h2 class="text-2xl font-bold mb-6">用户注册</h2>
    
    <!-- 用户名 -->
    <FormInput
      ref="usernameInput"
      v-model="formData.username"
      label="用户名"
      placeholder="请输入用户名"
      required
      :rules="usernameRules"
      helpText="用户名将作为您的唯一标识"
      successMessage="用户名可用"
    />
    
    <!-- 邮箱 -->
    <FormInput
      ref="emailInput"
      v-model="formData.email"
      label="邮箱"
      type="email"
      placeholder="请输入邮箱"
      required
      :rules="emailRules"
      helpText="我们会向此邮箱发送验证邮件"
      successMessage="邮箱格式正确"
    />
    
    <!-- 密码 -->
    <FormInput
      ref="passwordInput"
      v-model="formData.password"
      label="密码"
      type="password"
      placeholder="请输入密码"
      required
      :rules="passwordRules"
      helpText="密码至少8个字符，包含大写字母和数字"
    />
    
    <!-- 确认密码 -->
    <FormInput
      ref="confirmPasswordInput"
      v-model="formData.confirmPassword"
      label="确认密码"
      type="password"
      placeholder="请再次输入密码"
      required
      :rules="confirmPasswordRules"
    />
    
    <!-- 手机号 -->
    <FormInput
      ref="phoneInput"
      v-model="formData.phone"
      label="手机号"
      type="tel"
      placeholder="请输入手机号"
      :rules="phoneRules"
      helpText="选填，用于账号安全验证"
    />
    
    <!-- 提交按钮 -->
    <button 
      type="submit" 
      class="btn btn-primary w-full mt-6"
      :disabled="isSubmitting"
    >
      {{ isSubmitting ? '提交中...' : '注册' }}
    </button>
  </form>
</template>

<script setup>
import { ref, reactive } from 'vue'
import FormInput from '@/components/FormInput.vue'
import { ElMessage } from 'element-plus'

// 表单数据
const formData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  phone: ''
})

// 提交状态
const isSubmitting = ref(false)

// 表单引用
const usernameInput = ref(null)
const emailInput = ref(null)
const passwordInput = ref(null)
const confirmPasswordInput = ref(null)
const phoneInput = ref(null)

// 验证规则
const usernameRules = [
  (v) => v.length >= 3 || '用户名至少3个字符',
  (v) => v.length <= 20 || '用户名最多20个字符',
  (v) => /^[a-zA-Z0-9_]+$/.test(v) || '用户名只能包含字母、数字和下划线'
]

const emailRules = [
  (v) => /.+@.+\..+/.test(v) || '请输入有效的邮箱地址'
]

const passwordRules = [
  (v) => v.length >= 8 || '密码至少8个字符',
  (v) => /[A-Z]/.test(v) || '密码必须包含大写字母',
  (v) => /[0-9]/.test(v) || '密码必须包含数字'
]

const confirmPasswordRules = [
  (v) => v === formData.password || '两次输入的密码不一致'
]

const phoneRules = [
  (v) => !v || /^1[3-9]\d{9}$/.test(v) || '请输入有效的手机号'
]

// 表单提交
const handleSubmit = async () => {
  // 验证所有字段
  const isUsernameValid = usernameInput.value.validate()
  const isEmailValid = emailInput.value.validate()
  const isPasswordValid = passwordInput.value.validate()
  const isConfirmPasswordValid = confirmPasswordInput.value.validate()
  const isPhoneValid = phoneInput.value.validate()
  
  if (!isUsernameValid || !isEmailValid || !isPasswordValid || !isConfirmPasswordValid || !isPhoneValid) {
    ElMessage.error('请检查表单填写是否正确')
    return
  }
  
  isSubmitting.value = true
  
  try {
    // 调用注册API
    // await register(formData)
    
    ElMessage.success('注册成功！')
    
    // 重置表单
    Object.keys(formData).forEach(key => {
      formData[key] = ''
    })
    
    // 重置验证状态
    usernameInput.value.reset()
    emailInput.value.reset()
    passwordInput.value.reset()
    confirmPasswordInput.value.reset()
    phoneInput.value.reset()
    
  } catch (error) {
    ElMessage.error(error.message || '注册失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}
</script>
```

## 方法

组件暴露了以下方法，可通过 ref 调用：

### validate()

手动触发验证，返回验证结果 (true/false)

```vue
<script setup>
const inputRef = ref(null)

const checkValid = () => {
  const isValid = inputRef.value.validate()
  console.log('验证结果:', isValid)
}
</script>
```

### reset()

重置验证状态，清除错误消息

```vue
<script setup>
const inputRef = ref(null)

const resetForm = () => {
  inputRef.value.reset()
}
</script>
```

## 事件

### @validate

验证状态改变时触发，参数为验证结果 (true/false)

```vue
<FormInput
  v-model="email"
  @validate="handleValidate"
/>

<script setup>
const handleValidate = (isValid) => {
  console.log('验证状态:', isValid)
}
</script>
```

## 样式定制

组件使用 CSS 变量，可通过覆盖变量来定制样式：

```css
/* 自定义成功色 */
:root {
  --success-color: #10b981;
  --error-color: #ef4444;
}

/* 自定义输入框样式 */
.input {
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
}
```

## 无障碍访问

组件内置了完整的无障碍支持：

- ✅ 正确的 ARIA 标签 (aria-invalid, aria-describedby)
- ✅ 错误消息关联 (role="alert")
- ✅ 键盘导航支持
- ✅ 焦点管理

## 最佳实践

### 1. 合理使用必填标记

```vue
<!-- ✅ 推荐：必填字段显示星号 -->
<FormInput
  v-model="email"
  label="邮箱"
  required
/>

<!-- ❌ 不推荐：所有字段都标记必填 -->
```

### 2. 提供清晰的帮助文本

```vue
<!-- ✅ 推荐：提供有用的帮助信息 -->
<FormInput
  v-model="password"
  label="密码"
  type="password"
  helpText="密码至少8个字符，包含大写字母和数字"
/>

<!-- ❌ 不推荐：帮助文本过于简单或缺失 -->
<FormInput
  v-model="password"
  label="密码"
  type="password"
/>
```

### 3. 验证规则清晰明确

```vue
<!-- ✅ 推荐：错误消息清晰具体 -->
const passwordRules = [
  (v) => v.length >= 8 || '密码至少8个字符',
  (v) => /[A-Z]/.test(v) || '密码必须包含大写字母'
]

<!-- ❌ 不推荐：错误消息模糊 -->
const passwordRules = [
  (v) => v.length >= 8 || '密码不符合要求'
]
```

### 4. 表单提交前统一验证

```vue
<script setup>
const handleSubmit = () => {
  // ✅ 推荐：提交前验证所有字段
  const isValid = [
    usernameInput.value.validate(),
    emailInput.value.validate(),
    passwordInput.value.validate()
  ].every(Boolean)
  
  if (!isValid) {
    ElMessage.error('请检查表单填写')
    return
  }
  
  // 提交表单...
}
</script>
```

## 常见问题

### Q: 如何实现异步验证？

A: 可以在验证规则中返回 Promise：

```vue
<script setup>
const usernameRules = [
  async (v) => {
    if (!v) return '用户名不能为空'
    
    // 异步检查用户名是否已存在
    const exists = await checkUsernameExists(v)
    return !exists || '用户名已存在'
  }
]
</script>
```

### Q: 如何自定义错误消息样式？

A: 覆盖 `.form-feedback.is-invalid` 样式：

```css
.form-feedback.is-invalid {
  color: #dc2626;
  font-weight: 500;
}
```

### Q: 如何禁用实时验证？

A: 组件默认在失焦后才开始实时验证，首次输入不会触发验证。如需完全禁用实时验证，可以修改组件源码中的 `handleInput` 方法。

---

**更新日期**: 2026-02-11  
**版本**: 1.0.0
