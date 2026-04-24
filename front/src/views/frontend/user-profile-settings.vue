<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden">
            <div class="bg-primary-subtle border-b border-border-color p-6">
                <h1 class="text-2xl font-bold text-text-primary">个人资料设置</h1>
                <p class="text-text-secondary mt-2">编辑您的个人信息</p>
            </div>
            <div class="p-6">
                <form class="space-y-6" @submit.prevent="submitForm">
                    <!-- 头像上传 -->
                    <div class="flex flex-col md:flex-row items-center gap-6">
                        <div class="flex-shrink-0">
                            <div class="relative">
                                <img 
                                    class="w-32 h-32 rounded-full object-cover border-4 border-background-primary shadow-lg" 
                                    :src="getAvatarUrl" 
                                    :alt="store.user.username"
                                >
                                <div class="absolute bottom-0 right-0">
                                    <button 
                                        type="button"
                                        class="w-10 h-10 rounded-full bg-primary-color text-white flex items-center justify-center shadow-md hover:bg-primary-dark transition-all duration-300"
                                        @click="triggerFileInput"
                                    >
                                        <svg class="w-5 h-5" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10H7m0 0a2 2 0 1 0 0 4h6a2 2 0 1 0 0-4ZM7 10a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2m0 0a2 2 0 1 0 0 4H9a2 2 0 1 0 0-4Z" />
                                        </svg>
                                    </button>
                                    <input 
                                        ref="fileInput"
                                        type="file"
                                        accept="image/*"
                                        class="hidden"
                                        @change="handleFileUpload"
                                    >
                                </div>
                            </div>
                        </div>
                        <div class="flex-1">
                            <p class="text-sm text-text-secondary mb-2">
                                上传头像图片，建议尺寸 200x200px
                            </p>
                            <p class="text-xs text-text-tertiary">
                                支持 JPG、PNG、WEBP 格式，文件大小不超过 2MB
                            </p>
                        </div>
                    </div>
                    
                    <!-- 基本信息 -->
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                用户名
                            </label>
                            <input 
                                v-model="form.username"
                                type="text"
                                class="input input-outline w-full"
                                placeholder="请输入用户名"
                            >
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                邮箱
                            </label>
                            <input 
                                v-model="form.email"
                                type="email"
                                class="input input-outline w-full"
                                placeholder="请输入邮箱"
                            >
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                手机号
                            </label>
                            <input 
                                v-model="form.phone"
                                type="tel"
                                class="input input-outline w-full"
                                placeholder="请输入手机号"
                            >
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                昵称
                            </label>
                            <input 
                                v-model="form.nickname"
                                type="text"
                                class="input input-outline w-full"
                                placeholder="请输入昵称"
                            >
                        </div>
                    </div>
                    
                    <!-- 个人简介 -->
                    <div>
                        <label class="block text-sm font-medium text-text-primary mb-2">
                            个人简介
                        </label>
                        <textarea 
                            v-model="form.bio"
                            class="input input-outline w-full"
                            placeholder="请输入个人简介"
                            rows="4"
                        ></textarea>
                    </div>
                    
                    <!-- 社交链接 -->
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                GitHub
                            </label>
                            <input 
                                v-model="form.github"
                                type="url"
                                class="input input-outline w-full"
                                placeholder="请输入 GitHub 链接"
                            >
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                个人网站
                            </label>
                            <input 
                                v-model="form.website"
                                type="url"
                                class="input input-outline w-full"
                                placeholder="请输入个人网站链接"
                            >
                        </div>
                    </div>
                    
                    <!-- 提交按钮 -->
                    <div class="flex justify-end gap-3">
                        <button 
                            type="button"
                            class="btn btn-outline px-6 py-2"
                            @click="resetForm"
                        >
                            重置
                        </button>
                        <button 
                            type="submit"
                            class="btn btn-primary px-6 py-2"
                            :disabled="isSubmitting"
                        >
                            {{ isSubmitting ? '保存中...' : '保存修改' }}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

import logger from '@/utils/logger'
const store = useAuthStore()
const fileInput = ref(null)

// 表单数据
const form = ref({
    username: '',
    email: '',
    phone: '',
    nickname: '',
    bio: '',
    github: '',
    website: '',
    avatar: ''
})

// 提交状态
const isSubmitting = ref(false)

// 计算头像URL
const getAvatarUrl = computed(() => {
    if (form.value.avatar) {
        if (form.value.avatar.startsWith('http://') || form.value.avatar.startsWith('https://')) {
            return form.value.avatar
        } else if (form.value.avatar) {
            return `http://localhost:8080/${form.value.avatar}`
        }
    }
    if (store.isLoggedIn && store.user.avatar) {
        const avatar = store.user.avatar
        if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
            return avatar
        } else if (avatar) {
            return `http://localhost:8080/${avatar}`
        }
    }
    return new URL('@/assets/头像.jpg', import.meta.url).href
})

// 触发文件输入
const triggerFileInput = () => {
    fileInput.value?.click()
}

// 处理文件上传
const handleFileUpload = (event) => {
    const file = event.target.files[0]
    if (file) {
        // 这里应该实现文件上传逻辑
        logger.debug('上传文件:', file)
        // 模拟上传成功
        form.value.avatar = URL.createObjectURL(file)
    }
}

// 提交表单
const submitForm = async () => {
    try {
        isSubmitting.value = true
        // 这里应该实现表单提交逻辑
        logger.debug('提交表单:', form.value)
        // 模拟提交成功
        setTimeout(() => {
            isSubmitting.value = false
            // 更新store中的用户信息
            store.setUser({
                ...store.user,
                ...form.value
            })
            ElMessage.success('个人资料更新成功')
        }, 1000)
    } catch (error) {
        isSubmitting.value = false
        logger.error('提交失败:', error)
        ElMessage.error('提交失败，请重试')
    }
}

// 重置表单
const resetForm = () => {
    if (store.isLoggedIn()) {
        form.value = {
            username: store.user.username || '',
            email: store.user.email || '',
            phone: store.user.phone || '',
            nickname: store.user.nickname || '',
            bio: store.user.bio || '',
            github: store.user.github || '',
            website: store.user.website || '',
            avatar: store.user.avatar || ''
        }
    }
}

// 组件挂载时初始化表单
onMounted(() => {
    resetForm()
})
</script>

<style scoped>
/* 响应式调整 */
@media (max-width: 768px) {
    .p-6 {
        padding: 1rem;
    }
    
    .gap-6 {
        gap: 1.25rem;
    }
    
    .w-32.h-32 {
        width: 8rem;
        height: 8rem;
    }
    
    .text-sm {
        font-size: 0.875rem;
    }
    
    .text-xs {
        font-size: 0.75rem;
    }
    
    .mb-2 {
        margin-bottom: 0.5rem;
    }
    
    .grid-cols-1.md\:grid-cols-2 {
        grid-template-columns: 1fr;
    }
}
</style>
