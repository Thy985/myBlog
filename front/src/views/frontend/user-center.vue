<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <!-- 响应式布局容器 -->
        <div class="flex flex-col lg:flex-row gap-6">
            <!-- 左侧导航菜单 - 桌面端显示 -->
            <div class="lg:w-64 flex-shrink-0">
                <div class="bg-background-primary border border-border-color rounded-xl shadow-md p-6 sticky top-8">
                    <h3 class="text-lg font-bold text-text-primary mb-6">个人中心</h3>
                    <nav>
                        <ul class="space-y-1">
                            <!-- 内容管理 -->
                            <li>
                                <div class="text-sm font-medium text-text-secondary mb-2 px-3">内容管理</div>
                                <ul class="space-y-1">
                                    <li>
                                        <router-link 
                                            to="/user/articles/create"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10H7m0 0a2 2 0 1 0 0 4h6a2 2 0 1 0 0-4ZM7 10a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2m0 0a2 2 0 1 0 0 4H9a2 2 0 1 0 0-4Z" />
                                            </svg>
                                            发布文章
                                        </router-link>
                                    </li>
                                    <li>
                                        <router-link 
                                            to="/user/articles"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 10a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12V6m0 0l3 3m-3-3l3-3" />
                                            </svg>
                                            我的文章
                                        </router-link>
                                    </li>
                                    <li>
                                        <router-link 
                                            to="/user/categories"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H4Zm12 12H4m10-7h-4m4 0-4-4m4 4-4 4" />
                                            </svg>
                                            我的分类
                                        </router-link>
                                    </li>
                                    <li>
                                        <router-link 
                                            to="/user/tags"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.394 2.08a1 1 0 0 0-.788 0l-7 3a1 1 0 0 0 0 1.84L5.25 8.051a.999.999 0 0 1 .356-.257l4-1.714a1 1 0 1 1 .788 1.838L7.667 9.088l1.94.831a1 1 0 0 0 .787 0l7-3a1 1 0 0 0 0-1.838l-7-3Z" />
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2 12a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v2Z" />
                                            </svg>
                                            我的标签
                                        </router-link>
                                    </li>
                                    <li>
                                        <router-link 
                                            to="/user/media"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.994 19.944a1 1 0 0 0 1.05-.58l1.006-1.937a1 1 0 0 0-.364-1.293L16.273 15.09l-1.91-1.021a2.995 2.995 0 0 0-1.998-.524 2.995 2.995 0 0 0-1.998.524l-1.91 1.021-1.056 1.056a1 1 0 0 0-.28.618l-.154 1.543a1 1 0 0 0 1.182 1.182l1.543-.154a1 1 0 0 0 .618-.28l1.056-1.056 1.021 1.91a1 1 0 0 0 1.293.364l1.937-1.006a1 1 0 0 0 .58-1.05Z" />
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.05 3.05a2.5 2.5 0 0 1 3.535 0l1.414 1.414a2.5 2.5 0 0 1 0 3.535l-8.586 8.586A2 2 0 0 0 4 18h4a1 1 0 0 0 1-1v-4a2.5 2.5 0 0 1 0-3.535l1.414-1.414a2.5 2.5 0 0 1 3.535 0l4.243 4.243a1 1 0 0 0 1.414 0l2.829-2.829a1 1 0 0 0 0-1.414l-4.243-4.243a2.5 2.5 0 0 1 0-3.535l1.414-1.414a2.5 2.5 0 0 1 3.535 0l1.414 1.414a2.5 2.5 0 0 1 0 3.535l-.822.822" />
                                            </svg>
                                            我的媒体库
                                        </router-link>
                                    </li>
                                </ul>
                            </li>
                            
                            <!-- 互动管理 -->
                            <li class="mt-8">
                                <div class="text-sm font-medium text-text-secondary mb-2 px-3">互动管理</div>
                                <ul class="space-y-1">
                                    <li>
                                        <router-link 
                                            to="/user/comments"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 0 1 0 2.828l-7 7a2 2 0 0 1-2.828 0l-7-7A1.994 1.994 0 0 1 3 12V7a4 4 0 0 1 4-4Z" />
                                            </svg>
                                            我的评论
                                        </router-link>
                                    </li>
                                    <li>
                                        <router-link 
                                            to="/user/collections"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.342 3.036A1 1 0 0 1 10 4v10a1 1 0 0 1-1.707.707l-4.646-4.646A1 1 0 0 1 4 10V6a1 1 0 0 1 1.342-.964l4-1Z" />
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 4v10a1 1 0 0 1-1.707.707l-4.646-4.646A1 1 0 0 1 10 10V6a1 1 0 0 1 1.342-.964l4-1a1 1 0 0 1 .658.658Z" />
                                            </svg>
                                            我的收藏
                                        </router-link>
                                    </li>
                                </ul>
                            </li>
                            
                            <!-- 个人设置 -->
                            <li class="mt-8">
                                <div class="text-sm font-medium text-text-secondary mb-2 px-3">个人设置</div>
                                <ul class="space-y-1">
                                    <li>
                                        <router-link 
                                            to="/user/settings/profile"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19 11h-6a3 3 0 0 0-3 3v4a3 3 0 0 0 3 3h6a3 3 0 0 0 3-3v-4a3 3 0 0 0-3-3Zm0 0h-6a3 3 0 0 0-3-3 3 3 0 0 0-3 3H5a3 3 0 0 0-3 3v4a3 3 0 0 0 3 3h6a3 3 0 0 0 3-3v-4a3 3 0 0 0-3-3Zm0 0h-6" />
                                            </svg>
                                            个人资料
                                        </router-link>
                                    </li>
                                    <li>
                                        <router-link 
                                            to="/user/settings/account"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11V6a2 2 0 0 0-2-2h1a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1h1a2 2 0 0 0 2 2h1M7 8h10M7 12h4m-4 4h4" />
                                            </svg>
                                            账户设置
                                        </router-link>
                                    </li>
                                    <li>
                                        <router-link 
                                            to="/user/settings"
                                            class="nav-link"
                                        >
                                            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                                            </svg>
                                            智能体设置
                                        </router-link>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </nav>
                </div>
            </div>
            
            <!-- 右侧内容区域 -->
            <div class="flex-1">
                <!-- 个人信息卡片 -->
                <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden mb-6">
                    <div class="bg-primary-subtle border-b border-border-color p-6">
                        <h1 class="text-2xl font-bold text-text-primary">个人中心</h1>
                        <p class="text-text-secondary mt-2">管理您的个人信息和内容</p>
                    </div>
                    <div class="p-6">
                        <div class="flex flex-col md:flex-row items-center md:items-start gap-6">
                            <div class="flex-shrink-0">
                                <img 
                                    class="w-24 h-24 rounded-full object-cover border-4 border-background-primary shadow-lg" 
                                    :src="getAvatarUrl" 
                                    :alt="store.user.username"
                                    loading="lazy"
                                >
                            </div>
                            <div class="flex-1">
                                <h2 class="text-xl font-bold text-text-primary">{{ store.user.username }}</h2>
                                <p class="text-text-secondary mt-1">{{ store.user.email || '未设置邮箱' }}</p>
                                <div class="flex flex-wrap gap-2 mt-4">
                                    <span class="text-xs font-medium text-text-tertiary bg-primary-subtle px-3 py-1 rounded-full">
                                        {{ store.user.role || '普通用户' }}
                                    </span>
                                    <span class="text-xs font-medium text-text-tertiary bg-primary-subtle px-3 py-1 rounded-full">
                                        注册时间: {{ formatDate(store.user.createdAt) }}
                                    </span>
                                </div>
                            </div>
                            <div>
                                <router-link 
                                    to="/user/settings/profile"
                                    class="btn btn-outline px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                                >
                                    编辑资料
                                </router-link>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- 统计信息 -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                    <div class="bg-background-primary border border-border-color rounded-xl shadow-md p-6 hover:shadow-lg transition-all duration-300">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-text-secondary text-sm">发布文章</p>
                                <h3 class="text-3xl font-bold text-primary-color mt-1">{{ articleCount }}</h3>
                            </div>
                            <div class="w-12 h-12 rounded-full bg-primary-subtle flex items-center justify-center">
                                <svg class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10H7m0 0a2 2 0 1 0 0 4h6a2 2 0 1 0 0-4ZM7 10a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2m0 0a2 2 0 1 0 0 4H9a2 2 0 1 0 0-4Z" />
                                </svg>
                            </div>
                        </div>
                    </div>
                    <div class="bg-background-primary border border-border-color rounded-xl shadow-md p-6 hover:shadow-lg transition-all duration-300">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-text-secondary text-sm">收到评论</p>
                                <h3 class="text-3xl font-bold text-primary-color mt-1">{{ commentCount }}</h3>
                            </div>
                            <div class="w-12 h-12 rounded-full bg-primary-subtle flex items-center justify-center">
                                <svg class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 0 1 0 2.828l-7 7a2 2 0 0 1-2.828 0l-7-7A1.994 1.994 0 0 1 3 12V7a4 4 0 0 1 4-4Z" />
                                </svg>
                            </div>
                        </div>
                    </div>
                    <div class="bg-background-primary border border-border-color rounded-xl shadow-md p-6 hover:shadow-lg transition-all duration-300">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-text-secondary text-sm">获得点赞</p>
                                <h3 class="text-3xl font-bold text-primary-color mt-1">{{ likeCount }}</h3>
                            </div>
                            <div class="w-12 h-12 rounded-full bg-primary-subtle flex items-center justify-center">
                                <svg class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 0 0 .95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 0 0-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 0 0-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 0 0-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 0 0 .951-.69l1.519-4.674Z" />
                                </svg>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- 最近活动 -->
                <div class="bg-background-primary border border-border-color rounded-xl shadow-md p-6 mb-6">
                    <h3 class="text-lg font-bold text-text-primary mb-4">最近活动</h3>
                    <div class="space-y-4">
                        <div v-if="recentActivities.length === 0" class="text-center py-8 text-text-secondary">
                            暂无活动记录
                        </div>
                        <div v-for="activity in recentActivities" :key="activity.id" class="flex gap-4 p-4 border border-border-color rounded-lg hover:bg-primary-subtle transition-all duration-300">
                            <div class="flex-shrink-0 w-10 h-10 rounded-full bg-primary-subtle flex items-center justify-center">
                                <svg class="w-5 h-5 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10H7m0 0a2 2 0 1 0 0 4h6a2 2 0 1 0 0-4ZM7 10a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2m0 0a2 2 0 1 0 0 4H9a2 2 0 1 0 0-4Z" />
                                </svg>
                            </div>
                            <div class="flex-1">
                                <p class="text-text-primary">{{ activity.description }}</p>
                                <p class="text-xs text-text-tertiary mt-1">{{ formatDate(activity.createdAt) }}</p>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- 快捷操作 -->
                <div class="bg-background-primary border border-border-color rounded-xl shadow-md p-6">
                    <h3 class="text-lg font-bold text-text-primary mb-4">快捷操作</h3>
                    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <router-link 
                            to="/user/articles/create"
                            class="p-4 text-center border border-border-color rounded-lg hover:bg-primary-subtle transition-all duration-300"
                        >
                            <svg class="w-8 h-8 mx-auto text-primary-color mb-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10H7m0 0a2 2 0 1 0 0 4h6a2 2 0 1 0 0-4ZM7 10a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2m0 0a2 2 0 1 0 0 4H9a2 2 0 1 0 0-4Z" />
                            </svg>
                            <span class="font-medium text-text-primary">发布文章</span>
                        </router-link>
                        <router-link 
                            to="/user/articles"
                            class="p-4 text-center border border-border-color rounded-lg hover:bg-primary-subtle transition-all duration-300"
                        >
                            <svg class="w-8 h-8 mx-auto text-primary-color mb-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 10a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12V6m0 0l3 3m-3-3l3-3" />
                            </svg>
                            <span class="font-medium text-text-primary">管理文章</span>
                        </router-link>
                        <router-link 
                            to="/user/comments"
                            class="p-4 text-center border border-border-color rounded-lg hover:bg-primary-subtle transition-all duration-300"
                        >
                            <svg class="w-8 h-8 mx-auto text-primary-color mb-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 0 1 0 2.828l-7 7a2 2 0 0 1-2.828 0l-7-7A1.994 1.994 0 0 1 3 12V7a4 4 0 0 1 4-4Z" />
                            </svg>
                            <span class="font-medium text-text-primary">我的评论</span>
                        </router-link>
                        <router-link 
                            to="/user/settings/profile"
                            class="p-4 text-center border border-border-color rounded-lg hover:bg-primary-subtle transition-all duration-300"
                        >
                            <svg class="w-8 h-8 mx-auto text-primary-color mb-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19 11h-6a3 3 0 0 0-3 3v4a3 3 0 0 0 3 3h6a3 3 0 0 0 3-3v-4a3 3 0 0 0-3-3Zm0 0h-6a3 3 0 0 0-3-3 3 3 0 0 0-3 3H5a3 3 0 0 0-3 3v4a3 3 0 0 0 3 3h6a3 3 0 0 0 3-3v-4a3 3 0 0 0-3-3Zm0 0h-6" />
                            </svg>
                            <span class="font-medium text-text-primary">编辑资料</span>
                        </router-link>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useMainStore } from '@/stores'
import { getUserArticles } from '@/api/frontend/article'
import { getCommentList } from '@/api/frontend/comment'
import { getUserCollects } from '@/api/frontend/article'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const store = useMainStore()

const articleCount = ref(0)
const commentCount = ref(0)
const likeCount = ref(0)
const recentActivities = ref([])
const loading = ref(false)

const getAvatarUrl = computed(() => {
  if (store.isLoggedIn() && store.user.avatar) {
    const avatar = store.user.avatar
    if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
      return avatar
    } else if (avatar) {
      return `${import.meta.env.VITE_APP_BASE_URL || 'http://localhost:8080'}/${avatar}`
    }
  }
  return new URL('@/assets/头像.jpg', import.meta.url).href
})

const formatDate = (dateString) => {
  if (!dateString) {return '未知'}
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const fetchStats = async () => {
  try {
    loading.value = true
    const userId = store.user?.id
    if (!userId) {return}

    const [articlesRes, collectsRes] = await Promise.all([
      getUserArticles(userId, { page: 1, size: 1 }),
      getUserCollects({ page: 1, size: 1 })
    ])

    if (articlesRes.code === API_STATUS.SUCCESS) {
      articleCount.value = articlesRes.data?.total || 0
    }

    if (collectsRes.code === API_STATUS.SUCCESS) {
      likeCount.value = collectsRes.data?.total || 0
    }

    const commentsRes = await getCommentList(null, 1, 1)
    if (commentsRes.code === API_STATUS.SUCCESS) {
      commentCount.value = commentsRes.data?.total || 0
    }
  } catch (err) {
    logger.error('获取统计数据失败:', err.message)
  } finally {
    loading.value = false
  }
}

const fetchRecentActivities = async () => {
  try {
    const userId = store.user?.id
    if (!userId) {
      recentActivities.value = []
      return
    }

    const [articlesRes, commentsRes] = await Promise.all([
      getUserArticles(userId, { page: 1, size: 5 }),
      getCommentList(null, 1, 5)
    ])

    const activities = []

    if (articlesRes.code === API_STATUS.SUCCESS && articlesRes.data?.records) {
      articlesRes.data.records.slice(0, 3).forEach(article => {
        activities.push({
          id: article.id,
          description: `发布了文章《${article.title}》`,
          createdAt: article.createTime
        })
      })
    }

    if (commentsRes.code === API_STATUS.SUCCESS && commentsRes.data?.records) {
      commentsRes.data.records.slice(0, 2).forEach(comment => {
        activities.push({
          id: comment.id,
          description: `评论了文章《${comment.articleTitle || '未知文章'}》`,
          createdAt: comment.createTime
        })
      })
    }

    recentActivities.value = activities.sort((a, b) =>
      new Date(b.createdAt) - new Date(a.createdAt)
    ).slice(0, 5)
  } catch (err) {
    logger.error('获取最近活动失败:', err.message)
    recentActivities.value = []
  }
}

onMounted(() => {
  if (store.isLoggedIn()) {
    fetchStats()
    fetchRecentActivities()
  }
})
</script>

<style scoped>
/* 导航链接样式 */
.nav-link {
  @apply flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-all duration-300;
  color: var(--text-primary);
}

.nav-link:hover {
  background-color: var(--primary-subtle);
  color: var(--primary-color);
}

.nav-link.router-link-active {
  background-color: var(--primary-subtle);
  color: var(--primary-color);
  font-weight: 600;
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .lg\:w-64 {
    width: 100%;
  }
  
  .lg\:flex-row {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .mt-8 {
    margin-top: 1.5rem;
  }
  
  .p-6 {
    padding: 1rem;
  }
  
  .w-24.h-24 {
    width: 6rem;
    height: 6rem;
  }
  
  .text-2xl {
    font-size: 1.5rem;
  }
  
  .text-xl {
    font-size: 1.25rem;
  }
  
  .gap-6 {
    gap: 1.25rem;
  }
  
  .mt-6 {
    margin-top: 1.5rem;
  }
  
  .grid-cols-1.md\:grid-cols-3 {
    grid-template-columns: 1fr;
  }
  
  .grid-cols-2.md\:grid-cols-4 {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .flex-col.md\:flex-row {
    flex-direction: column;
    align-items: center;
  }
  
  .items-center.md\:items-start {
    align-items: center;
  }
}

@media (max-width: 480px) {
  .container {
    padding: 0 1rem;
  }
  
  .text-3xl {
    font-size: 1.5rem;
  }
  
  .w-24.h-24 {
    width: 5rem;
    height: 5rem;
  }
  
  .btn {
    padding: 0.5rem 1rem;
    font-size: 0.875rem;
  }
  
  .grid-cols-2.md\:grid-cols-4 {
    grid-template-columns: 1fr;
  }
}
</style>
