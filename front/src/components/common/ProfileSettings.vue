<template>
  <div class="bg-background-secondary border border-border-color rounded-xl p-6">
    <h2 class="text-lg font-bold text-text-primary mb-4">基本信息</h2>
    <form @submit.prevent="handleSubmit">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">用户名</label>
          <input
            v-model="formData.username"
            type="text"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-primary-color focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
            placeholder="请输入用户名"
          >
        </div>
        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">邮箱</label>
          <input
            v-model="formData.email"
            type="email"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-primary-color focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
            placeholder="请输入邮箱"
          >
        </div>
        <div class="md:col-span-2">
          <label class="block text-sm font-medium text-text-secondary mb-2">个人简介</label>
          <textarea
            v-model="formData.bio"
            rows="3"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-primary-color focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
            placeholder="请输入个人简介"
          ></textarea>
        </div>
      </div>
      <div class="mt-6">
        <button
          type="submit"
          class="btn btn-primary px-6 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
          :disabled="loading"
        >
          {{ loading ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  username: {
    type: String,
    default: ''
  },
  email: {
    type: String,
    default: ''
  },
  bio: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update'])

const formData = ref({
  username: '',
  email: '',
  bio: ''
})

const handleSubmit = async () => {
  emit('update', { ...formData.value })
}
</script>
