<template>
  <div class="bg-[var(--bg-card)] rounded-xl p-6 shadow-sm">
    <h2 class="text-lg font-bold text-[var(--text-primary)] mb-6">个人资料</h2>

    <div class="flex gap-8">
      <!-- 头像区域 -->
      <div class="flex-shrink-0 flex flex-col items-center">
        <div class="relative">
          <img
            class="w-24 h-24 rounded-full object-cover border-4 border-[var(--bg-secondary)] shadow-lg"
            :src="avatarUrl"
            :alt="formData.username || '用户'"
            @error="handleAvatarError"
          >
          <label
            class="absolute bottom-0 right-0 w-8 h-8 bg-[var(--color-primary)] rounded-full flex items-center justify-center cursor-pointer shadow-md hover:bg-[var(--color-primary)]/90 transition-colors"
            title="更换头像"
          >
            <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <input
              type="file"
              accept="image/*"
              class="hidden"
              @change="handleUpload"
            >
          </label>
        </div>
        <p class="text-xs text-[var(--text-muted)] mt-2">点击更换头像</p>
      </div>

      <!-- 表单区域 -->
      <div class="flex-1">
        <form @submit.prevent="handleSubmit">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label for="username" class="block text-sm font-medium text-[var(--text-secondary)] mb-1.5">
                用户名 <span class="text-[var(--color-error)]">*</span>
              </label>
              <input
                id="username"
                v-model="formData.username"
                type="text"
                class="w-full px-3 py-2 border rounded-lg bg-[var(--bg-primary)] transition-all duration-200 outline-none text-sm"
                :class="errors.username ? 'border-[var(--color-error)]' : 'border-[var(--border-color)] focus:border-[var(--color-primary)]'"
                placeholder="请输入用户名"
                maxlength="30"
                @input="debouncedValidate"
              >
              <p v-if="errors.username" class="text-[var(--color-error)] text-xs mt-1">{{ errors.username }}</p>
              <p v-else class="text-xs text-[var(--text-muted)] mt-1">{{ formData.username.length }}/30</p>
            </div>
            <div>
              <label for="email" class="block text-sm font-medium text-[var(--text-secondary)] mb-1.5">
                邮箱 <span class="text-[var(--color-error)]">*</span>
              </label>
              <input
                id="email"
                v-model="formData.email"
                type="email"
                class="w-full px-3 py-2 border rounded-lg bg-[var(--bg-primary)] transition-all duration-200 outline-none text-sm"
                :class="errors.email ? 'border-[var(--color-error)]' : 'border-[var(--border-color)] focus:border-[var(--color-primary)]'"
                placeholder="请输入邮箱"
                @input="debouncedValidate"
              >
              <p v-if="errors.email" class="text-[var(--color-error)] text-xs mt-1">{{ errors.email }}</p>
            </div>
          </div>
          <div class="mt-4">
            <label for="bio" class="block text-sm font-medium text-[var(--text-secondary)] mb-1.5">个人简介</label>
            <textarea
              id="bio"
              v-model="formData.bio"
              rows="3"
              class="w-full px-3 py-2 border rounded-lg bg-[var(--bg-primary)] focus:border-[var(--color-primary)] outline-none transition-all duration-200 resize-none text-sm"
              placeholder="介绍一下你自己..."
              maxlength="200"
            ></textarea>
            <p class="text-xs text-[var(--text-muted)] mt-1">{{ formData.bio.length }}/200</p>
          </div>
          <div class="mt-4 flex items-center gap-3">
            <button
              type="submit"
              class="btn btn-primary px-5 py-1.5 text-sm font-medium"
              :disabled="loading || !!errors.username || !!errors.email"
            >
              {{ loading ? '保存中...' : '保存修改' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAvatar } from '@/composables/useAvatar'

const props = defineProps({
  username: { type: String, default: '' },
  email: { type: String, default: '' },
  bio: { type: String, default: '' },
  avatar: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update', 'upload'])

const formData = ref({
  username: props.username || '',
  email: props.email || '',
  bio: props.bio || ''
})

const errors = ref({ username: '', email: '' })
let debounceTimer = null

const { avatarUrl, handleAvatarError } = useAvatar(() => props.avatar)

const validate = () => {
  errors.value = { username: '', email: '' }
  let valid = true
  if (!formData.value.username.trim()) {
    errors.value.username = '用户名不能为空'
    valid = false
  } else if (formData.value.username.trim().length < 2) {
    errors.value.username = '用户名至少2个字符'
    valid = false
  }
  if (!formData.value.email.trim()) {
    errors.value.email = '邮箱不能为空'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.value.email)) {
    errors.value.email = '请输入有效的邮箱地址'
    valid = false
  }
  return valid
}

const debouncedValidate = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => validate(), 300)
}

const handleSubmit = () => {
  if (!validate()) { return }
  emit('update', { ...formData.value })
  ElMessage.success({ message: '个人资料保存成功', duration: 3000 })
}

const handleUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    emit('upload', file)
  }
}

watch(() => props.username, (v) => { formData.value.username = v || '' })
watch(() => props.email, (v) => { formData.value.email = v || '' })
watch(() => props.bio, (v) => { formData.value.bio = v || '' })
</script>
