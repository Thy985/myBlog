<template>
  <div class="bg-[var(--bg-card)] rounded-xl p-6 shadow-sm">
    <h2 class="text-lg font-bold text-text-primary mb-4">修改密码</h2>
    <form @submit.prevent="handleSubmit">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">当前密码</label>
          <div class="relative">
            <input
              v-model="formData.currentPassword"
              :type="showCurrent ? 'text' : 'password'"
              class="w-full px-4 py-2 pr-10 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200"
              placeholder="请输入当前密码"
            >
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-text-tertiary hover:text-text-primary" @click="showCurrent = !showCurrent">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path v-if="!showCurrent" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path v-if="!showCurrent" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                <path v-if="showCurrent" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
            </button>
          </div>
          <p v-if="errors.currentPassword" class="text-[var(--color-error)] text-xs mt-1">{{ errors.currentPassword }}</p>
        </div>

        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">新密码</label>
          <div class="relative">
            <input
              v-model="formData.newPassword"
              :type="showNew ? 'text' : 'password'"
              class="w-full px-4 py-2 pr-10 border rounded-lg bg-background-primary transition-all duration-200 outline-none"
              :class="errors.newPassword ? 'border-[var(--color-error)] focus:border-[var(--color-error)]' : 'border-border-color focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20'"
              placeholder="请输入新密码"
            >
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-text-tertiary hover:text-text-primary" @click="showNew = !showNew">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path v-if="!showNew" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path v-if="!showNew" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                <path v-if="showNew" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
            </button>
          </div>
          <div v-if="formData.newPassword" class="mt-2">
            <div class="flex gap-1 mb-2">
              <div v-for="i in 4" :key="i" class="h-1 flex-1 rounded-full transition-colors" :class="strengthColor(i)"></div>
            </div>
            <p class="text-xs mb-3" :class="strengthTextColor">{{ strengthText }}</p>
            <div class="grid grid-cols-2 gap-x-4 gap-y-1 text-xs">
              <div class="flex items-center gap-1" :class="passwordRules.length ? 'text-[var(--color-success)]' : 'text-text-tertiary'">
                <span>{{ passwordRules.length ? '✓' : '○' }}</span>
                <span>至少 8 个字符</span>
              </div>
              <div class="flex items-center gap-1" :class="passwordRules.upper ? 'text-[var(--color-success)]' : 'text-text-tertiary'">
                <span>{{ passwordRules.upper ? '✓' : '○' }}</span>
                <span>包含大写字母</span>
              </div>
              <div class="flex items-center gap-1" :class="passwordRules.lower ? 'text-[var(--color-success)]' : 'text-text-tertiary'">
                <span>{{ passwordRules.lower ? '✓' : '○' }}</span>
                <span>包含小写字母</span>
              </div>
              <div class="flex items-center gap-1" :class="passwordRules.number ? 'text-[var(--color-success)]' : 'text-text-tertiary'">
                <span>{{ passwordRules.number ? '✓' : '○' }}</span>
                <span>包含数字</span>
              </div>
            </div>
          </div>
          <p v-if="errors.newPassword" class="text-[var(--color-error)] text-xs mt-1">{{ errors.newPassword }}</p>
        </div>

        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">确认新密码</label>
          <input
            v-model="formData.confirmPassword"
            :type="showConfirm ? 'text' : 'password'"
            class="w-full px-4 py-2 border rounded-lg bg-background-primary transition-all duration-200 outline-none"
            :class="errors.confirmPassword ? 'border-[var(--color-error)] focus:border-[var(--color-error)]' : 'border-border-color focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20'"
            placeholder="请再次输入新密码"
          >
          <p v-if="errors.confirmPassword" class="text-[var(--color-error)] text-xs mt-1">{{ errors.confirmPassword }}</p>
        </div>
      </div>

      <div class="mt-6 flex items-center gap-3">
        <button
          type="submit"
          class="btn btn-primary px-6 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
          :disabled="loading"
        >
          {{ loading ? '修改中...' : '修改密码' }}
        </button>
        <button
          type="button"
          class="btn btn-secondary px-6 py-2 text-sm font-medium transition-all duration-300 hover:scale-105 border border-border-color"
          @click="handleReset"
        >
          取消
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({ loading: { type: Boolean, default: false } })
const emit = defineEmits(['update', 'clear'])

const formData = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })
const errors = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })
const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)

const strength = computed(() => {
  const p = formData.value.newPassword
  if (!p) { return 0 }
  let s = 0
  if (p.length >= 8) { s++ }
  if (/[A-Z]/.test(p)) { s++ }
  if (/[0-9]/.test(p)) { s++ }
  if (/[^A-Za-z0-9]/.test(p)) { s++ }
  return s
})

const passwordRules = computed(() => {
  const p = formData.value.newPassword || ''
  return {
    length: p.length >= 8,
    upper: /[A-Z]/.test(p),
    lower: /[a-z]/.test(p),
    number: /[0-9]/.test(p)
  }
})

const strengthText = computed(() => {
  return ['', '太弱', '较弱', '一般', '强'][strength.value]
})

const strengthTextColor = computed(() => {
  return ['', 'text-[var(--strength-weak)]', 'text-[var(--strength-fair)]', 'text-[var(--strength-good)]', 'text-[var(--strength-strong)]'][strength.value]
})

const strengthColor = (i) => {
  if (i > strength.value) { return 'bg-[var(--toggle-track)]' }
  return ['', 'bg-[var(--strength-weak)]', 'bg-[var(--strength-fair)]', 'bg-[var(--strength-good)]', 'bg-[var(--strength-strong)]'][strength.value]
}

const validate = () => {
  errors.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  let valid = true
  if (!formData.value.currentPassword) {
    errors.value.currentPassword = '请输入当前密码'
    valid = false
  }
  if (!formData.value.newPassword) {
    errors.value.newPassword = '请输入新密码'
    valid = false
  } else if (formData.value.newPassword.length < 8) {
    errors.value.newPassword = '密码至少8个字符'
    valid = false
  } else if (strength.value < 2) {
    errors.value.newPassword = '密码强度太弱，建议包含大小写字母、数字和特殊字符'
    valid = false
  }
  if (formData.value.newPassword !== formData.value.confirmPassword) {
    errors.value.confirmPassword = '两次输入的密码不一致'
    valid = false
  }
  if (formData.value.newPassword === formData.value.currentPassword) {
    errors.value.newPassword = '新密码不能与当前密码相同'
    valid = false
  }
  return valid
}

const handleSubmit = () => {
  if (!validate()) { return }
  emit('update', { ...formData.value })
  emit('clear')
  ElMessage.success({ message: '密码修改成功', duration: 3000 })
}

const handleReset = () => {
  formData.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  errors.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
}
</script>
