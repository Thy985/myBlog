<template>
  <div class="bg-background-secondary border border-border-color rounded-xl p-6">
    <h2 class="text-lg font-bold text-text-primary mb-4">隐私选项</h2>
    <div class="space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <h3 class="font-medium text-text-primary">公开个人资料</h3>
          <p class="text-sm text-text-secondary mt-1">允许其他用户查看您的个人资料</p>
        </div>
        <label class="relative inline-flex items-center cursor-pointer">
          <input
            v-model="formData.publicProfile"
            type="checkbox"
            class="sr-only peer"
          >
          <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-color"></div>
        </label>
      </div>
      <div class="flex items-center justify-between">
        <div>
          <h3 class="font-medium text-text-primary">接收评论通知</h3>
          <p class="text-sm text-text-secondary mt-1">当有新评论时接收邮件通知</p>
        </div>
        <label class="relative inline-flex items-center cursor-pointer">
          <input
            v-model="formData.commentNotifications"
            type="checkbox"
            class="sr-only peer"
          >
          <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-color"></div>
        </label>
      </div>
      <div class="flex items-center justify-between">
        <div>
          <h3 class="font-medium text-text-primary">接收系统通知</h3>
          <p class="text-sm text-text-secondary mt-1">接收网站系统通知</p>
        </div>
        <label class="relative inline-flex items-center cursor-pointer">
          <input
            v-model="formData.systemNotifications"
            type="checkbox"
            class="sr-only peer"
          >
          <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-color"></div>
        </label>
      </div>
    </div>
    <div class="mt-6">
      <button
        class="btn btn-primary px-6 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
        :disabled="loading"
        @click="handleSubmit"
      >
        {{ loading ? '保存中...' : '保存设置' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update'])

const formData = ref({
  publicProfile: true,
  commentNotifications: true,
  systemNotifications: true
})

const handleSubmit = async () => {
  emit('update', { ...formData.value })
}
</script>
