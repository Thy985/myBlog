<template>
    <header class="header-glass sticky top-0 z-50 transition-all duration-300">
        <nav class="max-w-screen-xl mx-auto px-4 sm:px-6 lg:px-8" aria-label="主要导航">
            <div class="flex items-center justify-between py-3 lg:py-4">
                <!-- Logo 和网站名称 -->
                <a href="/" class="flex items-center space-x-3" aria-label="返回首页">
                    <img
                        :src="store.setting.avatar || defaultLogo"
                        class="h-10 w-10 lg:h-12 lg:w-12 rounded-full object-cover transition-all duration-300 hover:scale-110 hover:shadow-md"
                        :alt="store.setting.blogName"
                        loading="lazy"
                        width="48"
                        height="48"
                        @error="(e) => e.target.src = defaultLogo"
                    />
                    <span class="hidden sm:block self-center text-lg lg:text-xl font-bold text-text-primary transition-all duration-300 hover:text-primary-color">
                        {{ store.setting.blogName }}
                    </span>
                </a>

                <!-- 右侧功能区 -->
                <div class="flex items-center gap-2 sm:gap-3 lg:gap-4">
                    <!-- 深色模式切换按钮 -->
                    <button
                        class="p-2 rounded-lg hover:bg-border-color transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary-color/20"
                        :aria-label="store.isDarkMode ? '切换到浅色模式' : '切换到深色模式'"
                        :aria-pressed="store.isDarkMode"
                        @click="toggleDarkMode"
                    >
                        <svg v-if="!store.isDarkMode" class="w-5 h-5 text-text-secondary transition-colors duration-200 hover:text-primary-color" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path>
                        </svg>
                        <svg v-else class="w-5 h-5 text-text-secondary transition-colors duration-200 hover:text-primary-color" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"></path>
                        </svg>
                    </button>

                    <!-- 搜索框组件 - 桌面端 -->
                    <div class="hidden md:block">
                        <SearchBar
                            v-model="searchKeyword"
                            @search="handleSearch"
                        />
                    </div>

                    <!-- 搜索按钮（移动端） -->
                    <button
                        type="button"
                        class="md:hidden p-2 rounded-lg hover:bg-border-color transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary-color/20"
                        @click="toggleMobileMenu"
                    >
                        <svg class="w-5 h-5 text-text-secondary" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z" />
                        </svg>
                    </button>

                    <!-- 登录/用户菜单 -->
                    <template v-if="!isLogin">
                        <button
                            class="btn btn-primary px-3 sm:px-4 py-1.5 sm:py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                            @click="$router.push('/login')"
                        >
                            登录
                        </button>
                    </template>
                    <template v-else>
                        <button
                            v-if="canPublish"
                            class="btn btn-primary px-3 sm:px-4 py-1.5 sm:py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                            @click="$router.push('/user/articles/create')"
                        >
                            发布
                        </button>
                        <UserMenu
                            :is-open="isUserMenuOpen"
                            :username="store.user.username"
                            :avatar="store.user.avatar"
                            :is-admin="isAdmin"
                            @toggle="toggleUserMenu"
                            @logout="openLogoutModal"
                        />
                    </template>
                </div>
            </div>

            <!-- 导航链接组件 -->
            <NavLinks />

            <!-- 移动端菜单组件 -->
            <MobileMenu
                v-model="searchKeyword"
                :is-open="isMobileMenuOpen"
                @search="handleSearch"
                @close="closeMobileMenu"
            />
        </nav>

        <!-- 退出登录模态框 -->
        <div
            v-if="isLogoutModalOpen"
            class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-50 transition-opacity duration-300"
            aria-modal="true"
            role="dialog"
            aria-labelledby="logout-modal-title"
            @click="closeLogoutModal"
        >
            <div
                class="w-full max-w-md bg-background-primary border border-border-color rounded-lg shadow-2xl dark:bg-background-primary transition-all duration-300 transform animate-fadeIn"
                role="document"
                @click.stop
            >
                <div class="p-6 text-center">
                    <svg class="mx-auto mb-4 w-12 h-12 text-warning-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11V6m0 8h.01M19 10a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                    </svg>
                    <h3 id="logout-modal-title" class="mb-4 text-lg font-medium text-text-primary">是否退出登录?</h3>
                    <div class="flex justify-center space-x-4 mt-6">
                        <button
                            class="btn btn-primary px-5 py-2.5 font-medium"
                            @click="handleLogout"
                        >
                            确认
                        </button>
                        <button
                            class="btn btn-outline px-5 py-2.5 font-medium"
                            @click="closeLogoutModal"
                        >
                            取消
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { showMessage } from '@/utils'
import { useMainStore } from '@/stores'
import SearchBar from '@/components/layout/SearchBar.vue'
import UserMenu from '@/components/layout/UserMenu.vue'
import NavLinks from '@/components/layout/NavLinks.vue'
import MobileMenu from '@/components/layout/MobileMenu.vue'

const store = useMainStore()
const router = useRouter()

const defaultLogo = new URL('@/assets/头像.jpg', import.meta.url).href

// 响应式数据
const searchKeyword = ref('')
const isUserMenuOpen = ref(false)
const isLogoutModalOpen = ref(false)
const isMobileMenuOpen = ref(false)

// 计算属性
const isLogin = computed(() => store.isLoggedIn())

const isAdmin = computed(() => {
  return store.user.role === 'admin'
})

const canPublish = computed(() => {
  return store.isLoggedIn()
})

// 方法
const toggleDarkMode = () => {
    store.toggleDarkMode()
}

const toggleUserMenu = () => {
    isUserMenuOpen.value = !isUserMenuOpen.value
}

const toggleMobileMenu = () => {
    isMobileMenuOpen.value = !isMobileMenuOpen.value
}

const closeMobileMenu = () => {
    isMobileMenuOpen.value = false
}

const openLogoutModal = () => {
    isUserMenuOpen.value = false
    isLogoutModalOpen.value = true
}

const closeLogoutModal = () => {
    isLogoutModalOpen.value = false
}

const handleSearch = (keyword) => {
    if (!keyword) {return}
    router.push({
        path: '/search',
        query: { keyword }
    })
}

const handleLogout = () => {
    store.logout()
    isLogoutModalOpen.value = false
    showMessage('退出登录成功', 'success')
}

const handleClickOutside = (event) => {
    if (!event.target.closest('#user-menu-button')) {
        isUserMenuOpen.value = false
    }
}

onMounted(() => {
    document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* Header 玻璃拟态效果 */
.header-glass {
  background: var(--glass-bg);
  border-bottom: 1px solid var(--glass-border);

  /* 玻璃拟态 - 支持时启用 */
  @supports (backdrop-filter: blur(20px)) {
    backdrop-filter: blur(var(--glass-blur-lg));
    -webkit-backdrop-filter: blur(var(--glass-blur-lg));
  }

  /* 不支持时的降级方案 */
  @supports not (backdrop-filter: blur(20px)) {
    background: var(--bg-primary);
  }
}

.dark .header-glass {
  background: rgba(10, 10, 11, 0.9);
  border-bottom: 1px solid var(--glass-border);

  @supports (backdrop-filter: blur(20px)) {
    backdrop-filter: blur(var(--glass-blur-lg));
    -webkit-backdrop-filter: blur(var(--glass-blur-lg));
  }

  @supports not (backdrop-filter: blur(20px)) {
    background: var(--bg-primary);
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fadeIn {
  animation: fadeIn 0.2s ease-out forwards;
}
</style>
