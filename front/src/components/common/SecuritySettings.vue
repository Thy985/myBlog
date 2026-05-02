<template>
  <div class="bg-[var(--bg-card)] rounded-xl p-6 shadow-sm">
    <!-- 安全评分 -->
    <div class="mb-6 p-4 bg-[var(--bg-secondary)] rounded-lg">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-3">
          <div
            class="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm"
            :class="securityScoreClass"
          >
            {{ securityScore }}
          </div>
          <div>
            <h3 class="font-medium text-[var(--text-primary)]">账户安全评分</h3>
            <p class="text-xs text-[var(--text-secondary)]">{{ securityLevelText }}</p>
          </div>
        </div>
      </div>
      <div class="w-full h-1.5 bg-[var(--toggle-track)] rounded-full overflow-hidden">
        <div
          class="h-full rounded-full transition-all duration-500"
          :class="securityProgressClass"
          :style="{ width: securityScore + '%' }"
        ></div>
      </div>
      <div class="mt-3 flex flex-wrap gap-x-4 gap-y-2 text-xs">
        <div class="flex items-center gap-1.5" :class="formData.mfaEnabled ? 'text-[var(--color-success)]' : 'text-[var(--text-muted)]'">
          <div class="w-1.5 h-1.5 rounded-full" :class="formData.mfaEnabled ? 'bg-[var(--color-success)]' : 'bg-[var(--text-muted)]'"></div>
          <span>多因素认证</span>
        </div>
        <div class="flex items-center gap-1.5" :class="formData.mfaSecret ? 'text-[var(--color-success)]' : 'text-[var(--text-muted)]'">
          <div class="w-1.5 h-1.5 rounded-full" :class="formData.mfaSecret ? 'bg-[var(--color-success)]' : 'bg-[var(--text-muted)]'"></div>
          <span>已配置 MFA</span>
        </div>
        <div class="flex items-center gap-1.5" :class="props.backupCodes?.length > 0 ? 'text-[var(--color-success)]' : 'text-[var(--text-muted)]'">
          <div class="w-1.5 h-1.5 rounded-full" :class="props.backupCodes?.length > 0 ? 'bg-[var(--color-success)]' : 'bg-[var(--text-muted)]'"></div>
          <span>备用验证码</span>
        </div>
        <div class="flex items-center gap-1.5 text-[var(--color-success)]">
          <div class="w-1.5 h-1.5 rounded-full bg-[var(--color-success)]"></div>
          <span>强密码保护</span>
        </div>
      </div>
    </div>

    <h2 class="text-lg font-bold text-text-primary mb-4">多因素认证 (MFA)</h2>
    <div class="mb-6">
      <p class="text-text-secondary mb-4">多因素认证为您的账户添加额外的安全层，即使密码泄露，未经授权的用户也无法访问您的账户。</p>
      <div class="flex items-center justify-between">
        <div>
          <h3 :id="'label-mfaEnabled'" class="font-medium text-text-primary">启用多因素认证</h3>
          <p class="text-sm text-text-secondary mt-1">登录时需要输入额外的验证码</p>
        </div>
        <label for="mfaEnabled" class="relative inline-flex items-center cursor-pointer">
          <input
            id="mfaEnabled"
            v-model="formData.mfaEnabled"
            type="checkbox"
            class="sr-only peer"
            aria-labelledby="label-mfaEnabled"
            @change="handleMfaToggle"
          >
          <div class="w-11 h-6 bg-[var(--toggle-track)] peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-[var(--toggle-border)] after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-color"></div>
        </label>
      </div>
    </div>

    <!-- MFA类型选择 -->
    <div v-if="formData.mfaEnabled" class="space-y-4">
      <h3 class="font-medium text-text-primary">选择验证方式</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
          class="border border-border-color rounded-lg p-4 cursor-pointer transition-all duration-200 hover:border-[var(--color-primary)]"
          :class="formData.mfaType === 'sms' ? 'border-[var(--color-primary)] bg-[var(--color-primary-subtle)]' : ''"
          @click="formData.mfaType = 'sms'"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="font-medium">短信验证</span>
            <input
              v-model="formData.mfaType"
              type="radio"
              name="mfaType"
              value="sms"
              class="form-radio text-[var(--color-primary)]"
            >
          </div>
          <p class="text-sm text-text-secondary">通过手机短信接收验证码</p>
        </div>
        <div
          class="border border-border-color rounded-lg p-4 cursor-pointer transition-all duration-200 hover:border-[var(--color-primary)]"
          :class="formData.mfaType === 'email' ? 'border-[var(--color-primary)] bg-[var(--color-primary-subtle)]' : ''"
          @click="formData.mfaType = 'email'"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="font-medium">邮箱验证</span>
            <input
              v-model="formData.mfaType"
              type="radio"
              name="mfaType"
              value="email"
              class="form-radio text-[var(--color-primary)]"
            >
          </div>
          <p class="text-sm text-text-secondary">通过邮箱接收验证码</p>
        </div>
        <div
          class="border border-border-color rounded-lg p-4 cursor-pointer transition-all duration-200 hover:border-[var(--color-primary)]"
          :class="formData.mfaType === 'app' ? 'border-[var(--color-primary)] bg-[var(--color-primary-subtle)]' : ''"
          @click="formData.mfaType = 'app'"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="font-medium">认证器应用</span>
            <input
              v-model="formData.mfaType"
              type="radio"
              name="mfaType"
              value="app"
              class="form-radio text-[var(--color-primary)]"
            >
          </div>
          <p class="text-sm text-text-secondary">使用 Google Authenticator 等应用</p>
        </div>
      </div>

      <!-- 认证器应用设置 -->
      <div v-if="formData.mfaType === 'app'" class="mt-6 p-4 border border-border-color rounded-lg bg-background-primary">
        <h4 class="font-medium text-text-primary mb-4">设置认证器应用</h4>
        <div v-if="!formData.mfaSecret" class="text-center py-4">
          <p class="text-text-secondary mb-4">点击生成密钥，然后使用认证器应用扫描二维码</p>
          <button
            class="btn btn-primary px-4 py-2 text-sm"
            :disabled="loading"
            @click="$emit('generate-secret')"
          >
            {{ loading ? '生成中...' : '生成密钥' }}
          </button>
        </div>
        <div v-else class="text-center">
          <div class="mb-4">
            <img
              :src="qrcodeUrl"
              alt="MFA 二维码"
              class="inline-block border border-border-color rounded-lg p-2 bg-white"
            >
          </div>
          <p class="text-text-secondary mb-4">使用认证器应用扫描二维码</p>
          <div class="mb-4">
            <h5 class="text-sm font-medium text-text-secondary mb-2">备用密钥</h5>
            <div class="bg-[var(--bg-secondary)] px-4 py-2 rounded font-mono text-sm mb-2">{{ formData.mfaSecret }}</div>
            <p class="text-xs text-text-tertiary">请保存此密钥，用于恢复 MFA 访问</p>
          </div>
          <div class="mt-4">
            <input
              v-model="formData.mfaCode"
              type="text"
              placeholder="输入认证器生成的验证码"
              class="w-full max-w-xs mx-auto px-4 py-2 border border-border-color rounded-lg text-center"
              maxlength="6"
            >
          </div>
          <button
            class="btn btn-primary px-4 py-2 text-sm mt-4"
            :disabled="loading || !formData.mfaCode"
            @click="$emit('verify-code', formData.mfaCode)"
          >
            {{ loading ? '验证中...' : '验证并保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 备用验证码管理 -->
    <div v-if="formData.mfaEnabled" class="mt-8 pt-6 border-t border-border-color">
      <h3 class="font-medium text-text-primary mb-4">备用验证码</h3>
      <p class="text-sm text-text-secondary mb-4">当您无法访问主要验证方式时，可以使用备用验证码登录。每个验证码只能使用一次。</p>

      <div v-if="!backupCodes.length" class="mb-4">
        <p class="text-text-secondary mb-4">您还没有生成备用验证码</p>
        <button
          class="btn btn-secondary px-4 py-2 text-sm"
          :disabled="loading"
          @click="$emit('generate-backup-codes')"
        >
          {{ loading ? '生成中...' : '生成备用验证码' }}
        </button>
      </div>

      <div v-else class="space-y-4">
        <div class="bg-warning-subtle border border-warning-border p-4 rounded-lg">
          <p class="text-warning-text text-sm">
            <i class="el-icon-warning-outline mr-2"></i>
            请妥善保存这些备用验证码，它们只会显示一次
          </p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div v-for="(code, index) in backupCodes" :key="index" class="bg-[var(--bg-secondary)] border border-border-color rounded-lg p-3">
            <code class="font-mono text-sm">{{ code }}</code>
          </div>
        </div>
        <div class="mt-4">
          <button
            class="btn btn-secondary px-4 py-2 text-sm"
            :disabled="loading"
            @click="$emit('regenerate-backup-codes')"
          >
            {{ loading ? '生成中...' : '重新生成备用验证码' }}
          </button>
          <p class="text-xs text-text-tertiary mt-2">重新生成将使所有现有备用验证码失效</p>
        </div>
      </div>
    </div>

    <!-- 保存按钮 -->
    <div v-if="formData.mfaEnabled && formData.mfaType !== 'app'" class="mt-6">
      <button
        class="btn btn-primary px-6 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
        :disabled="loading"
        @click="$emit('save-mfa')"
      >
        {{ loading ? '保存中...' : '保存设置' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  mfaEnabled: {
    type: Boolean,
    default: false
  },
  mfaType: {
    type: String,
    default: 'sms'
  },
  mfaSecret: {
    type: String,
    default: ''
  },
  backupCodes: {
    type: Array,
    default: () => []
  },
  username: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'update',
  'toggle',
  'generate-secret',
  'verify-code',
  'generate-backup-codes',
  'regenerate-backup-codes',
  'save-mfa'
])

const formData = ref({
  mfaEnabled: props.mfaEnabled,
  mfaType: props.mfaType,
  mfaSecret: props.mfaSecret,
  mfaCode: ''
})

const qrcodeUrl = computed(() => {
  if (!formData.value.mfaSecret) {return ''}
  const issuer = 'XingChenBlog'
  const account = props.username || 'user'
  const secret = formData.value.mfaSecret
  const otpAuthUrl = `otpauth://totp/${encodeURIComponent(issuer)}:${encodeURIComponent(account)}?secret=${secret}&issuer=${encodeURIComponent(issuer)}&algorithm=SHA1&digits=6&period=30`
  return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(otpAuthUrl)}`
})

const handleMfaToggle = () => {
  if (!formData.value.mfaEnabled) {
    formData.value.mfaType = ''
    formData.value.mfaSecret = ''
    formData.value.mfaCode = ''
    emit('save-mfa')
  }
}

const securityScore = computed(() => {
  let score = 50
  if (formData.value.mfaEnabled) { score += 25 }
  if (formData.value.mfaSecret) { score += 15 }
  if (props.backupCodes?.length > 0) { score += 10 }
  return score
})

const securityLevelText = computed(() => {
  if (securityScore.value >= 90) { return '非常安全' }
  if (securityScore.value >= 70) { return '较安全' }
  if (securityScore.value >= 50) { return '一般' }
  return '需改进'
})

const securityEmoji = computed(() => {
  if (securityScore.value >= 90) { return '🛡️' }
  if (securityScore.value >= 70) { return '✅' }
  if (securityScore.value >= 50) { return '⚠️' }
  return '🔴'
})

const securityScoreClass = computed(() => {
  if (securityScore.value >= 90) { return 'bg-[var(--color-success)]' }
  if (securityScore.value >= 70) { return 'bg-[var(--color-primary)]' }
  if (securityScore.value >= 50) { return 'bg-[var(--color-warning)]' }
  return 'bg-[var(--color-error)]'
})

const securityProgressClass = computed(() => {
  if (securityScore.value >= 90) { return 'bg-[var(--color-success)]' }
  if (securityScore.value >= 70) { return 'bg-[var(--color-primary)]' }
  if (securityScore.value >= 50) { return 'bg-[var(--color-warning)]' }
  return 'bg-[var(--color-error)]'
})
</script>
