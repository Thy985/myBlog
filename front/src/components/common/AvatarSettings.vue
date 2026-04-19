<template>
  <div class="bg-background-secondary border border-border-color rounded-xl p-6">
    <h2 class="text-lg font-bold text-text-primary mb-4">头像设置</h2>
    <div class="flex items-center gap-6">
      <div class="flex-shrink-0">
        <img
          class="w-20 h-20 rounded-full object-cover border-4 border-background-primary shadow-lg"
          :src="avatarUrl"
          :alt="username"
          loading="lazy"
          width="80"
          height="80"
          @error="handleAvatarError"
        >
      </div>
      <div class="flex-1">
        <p class="text-text-secondary mb-4">上传新头像</p>
        <input
          type="file"
          accept="image/*"
          class="block w-full text-sm text-text-secondary file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-primary-subtle file:text-primary-color hover:file:bg-primary-light transition-all duration-200"
          @change="handleUpload"
        >
        <p class="text-xs text-text-tertiary mt-2">支持 JPG、PNG、WebP 格式，最大 2MB</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAvatar } from '@/composables/useAvatar'

const props = defineProps({
  username: {
    type: String,
    default: ''
  },
  avatar: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['upload'])

const { avatarUrl, handleAvatarError } = useAvatar(() => props.avatar)

const handleUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    emit('upload', file)
  }
}
</script>
