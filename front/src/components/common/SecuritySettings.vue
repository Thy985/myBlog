<template>
  <div class="bg-background-secondary border border-border-color rounded-xl p-6">
    <h2 class="text-lg font-bold text-text-primary mb-4">多因素认证 (MFA)</h2>
    <div class="mb-6">
      <p class="text-text-secondary mb-4">多因素认证为您的账户添加额外的安全层，即使密码泄露，未经授权的用户也无法访问您的账户。</p>
      <div class="flex items-center justify-between">
        <div>
          <h3 class="font-medium text-text-primary">启用多因素认证</h3>
          <p class="text-sm text-text-secondary mt-1">登录时需要输入额外的验证码</p>
        </div>
        <label class="relative inline-flex items-center cursor-pointer">
          <input
            v-model="formData.mfaEnabled"
            type="checkbox"
            class="sr-only peer"
            @change="handleMfaToggle"
          >
          <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-color"></div>
        </label>
      </div>
    </div>

    <!-- MFA类型选择 -->
    <div v-if="formData.mfaEnabled" class="space-y-4">
      <h3 class="font-medium text-text-primary">选择验证方式</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
          class="border border-border-color rounded-lg p-4 cursor-pointer transition-all duration-200 hover:border-primary-color"
          :class="formData.mfaType === 'sms' ? 'border-primary-color bg-primary-subtle' : ''"
          @click="formData.mfaType = 'sms'"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="font-medium">短信验证</span>
            <input
              v-model="formData.mfaType"
              type="radio"
              name="mfaType"
              value="sms"
              class="form-radio text-primary-color"
            >
          </div>
          <p class="text-sm text-text-secondary">通过手机短信接收验证码</p>
        </div>
        <div
          class="border border-border-color rounded-lg p-4 cursor-pointer transition-all duration-200 hover:border-primary-color"
          :class="formData.mfaType === 'email' ? 'border-primary-color bg-primary-subtle' : ''"
          @click="formData.mfaType = 'email'"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="font-medium">邮箱验证</span>
            <input
              v-model="formData.mfaType"
              type="radio"
              name="mfaType"
              value="email"
              class="form-radio text-primary-color"
            >
          </div>
          <p class="text-sm text-text-secondary">通过邮箱接收验证码</p>
        </div>
        <div
          class="border border-border-color rounded-lg p-4 cursor-pointer transition-all duration-200 hover:border-primary-color"
          :class="formData.mfaType === 'app' ? 'border-primary-color bg-primary-subtle' : ''"
          @click="formData.mfaType = 'app'"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="font-medium">认证器应用</span>
            <input
              v-model="formData.mfaType"
              type="radio"
              name="mfaType"
              value="app"
              class="form-radio text-primary-color"
            >
          </div>
          <p class="text-sm text-text-secondary">使用Google Authenticator等应用</p>
        </div>
      </div>

      <!-- 认证器应用设置 -->
      <div v-if="formData.mfaType === 'app'" class="mt-6 p-4 border border-border-color rounded-lg bg-background-primary">
        <h4 class="font-medium text-text-primary mb-4">设置认证器应用</h4>
        <div v-if="!formData.mfaSecret" class="text-center py-4">
          <p class="text-text-secondary mb-4">点击生成二维码，然后使用认证器应用扫描</p>
          <button
            class="btn btn-primary px-4 py-2 text-sm"
            :disabled="loading"
            @click="$emit('generate-secret')"
          >
            {{ loading ? '生成中...' : '生成二维码' }}
          </button>
        </div>
        <div v-else class="text-center">
          <div class="mb-4">
            <img
              :src="qrcodeUrl"
              alt="MFA二维码"
              class="inline-block border border-border-color rounded-lg p-2 bg-white"
            >
          </div>
          <p class="text-text-secondary mb-4">使用认证器应用扫描二维码</p>
          <div class="mb-4">
            <h5 class="text-sm font-medium text-text-secondary mb-2">备用密钥</h5>
            <div class="bg-background-secondary px-4 py-2 rounded font-mono text-sm mb-2">{{ formData.mfaSecret }}</div>
            <p class="text-xs text-text-tertiary">请保存此密钥，用于恢复MFA访问</p>
          </div>
          <div class="mt-4">
            <input
              v-model="formData.mfaCode"
              type="text"
              placeholder="输入认证器应用生成的验证码"
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
          <div v-for="(code, index) in backupCodes" :key="index" class="bg-background-secondary border border-border-color rounded-lg p-3">
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
  const issuer = 'XingChen博客'
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
</script>
