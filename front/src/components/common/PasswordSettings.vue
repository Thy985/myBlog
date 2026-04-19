<template>
  <div class="bg-background-secondary border border-border-color rounded-xl p-6">
    <h2 class="text-lg font-bold text-text-primary mb-4">密码修改</h2>
    <form @submit.prevent="handleSubmit">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">当前密码</label>
          <input
            v-model="formData.currentPassword"
            type="password"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-primary-color focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
            placeholder="请输入当前密码"
          >
        </div>
        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">新密码</label>
          <input
            v-model="formData.newPassword"
            type="password"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-primary-color focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
            placeholder="请输入新密码"
          >
        </div>
        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">确认新密码</label>
          <input
            v-model="formData.confirmPassword"
            type="password"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-primary-color focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
            placeholder="请确认新密码"
          >
        </div>
      </div>
      <div class="mt-6">
        <button
          type="submit"
          class="btn btn-primary px-6 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
          :disabled="loading"
        >
          {{ loading ? '修改中...' : '修改密码' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { showMessage } from '@/utils'

defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update'])

const formData = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleSubmit = async () => {
  if (formData.value.newPassword !== formData.value.confirmPassword) {
    showMessage('两次输入的密码不一致', 'warning')
    return
  }
  emit('update', { ...formData.value })
  formData.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
}
</script>
