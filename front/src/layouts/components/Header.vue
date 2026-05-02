<template>
    <header class="header-glass" :class="{ 'scrolled': isScrolled }">
        <div class="scroll-progress" :style="{ width: `${scrollProgress}%` }"></div>

        <nav class="max-w-screen-xl mx-auto px-4 sm:px-6 lg:px-8" aria-label="主要导航">
            <div class="header-content">
                <a href="/" class="logo-wrapper" aria-label="返回首页">
                    <img
                        :src="logoUrl"
                        class="logo-image"
                        :alt="store.setting.blogName"
                        loading="lazy"
                        width="40"
                        height="40"
                        @error="(e) => e.target.src = defaultLogo"
                    />
                    <span class="logo-text">{{ store.setting.blogName }}</span>
                </a>

                <ul class="nav-links">
                    <li v-for="link in navItems" :key="link.path">
                        <a
                            :href="link.path"
                            class="nav-link"
                            :class="{ 'nav-link-active': isActive(link.path) }"
                        >
                            <span class="nav-link-text">{{ link.name }}</span>
                        </a>
                    </li>
                </ul>

                <div class="nav-actions">
                    <button
                        class="action-btn theme-btn"
                        :aria-label="store.isDarkMode ? '切换到浅色模式' : '切换到深色模式'"
                        @click="toggleDarkMode"
                    >
                        <svg v-if="!store.isDarkMode" class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path>
                        </svg>
                        <svg v-else class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"></path>
                        </svg>
                    </button>

                    <div class="hidden lg:flex search-wrapper">
                        <div class="search-bar">
                            <svg class="search-icon" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M9 3.5a5.5 5.5 0 100 11 5.5 5.5 0 000-11zM2 9a7 7 0 1112.452 4.391l3.328 3.329a.75.75 0 11-1.06 1.06l-3.329-3.328A7 7 0 012 9z" clip-rule="evenodd" />
                            </svg>
                            <input
                                v-model="searchKeyword"
                                type="search"
                                class="search-input"
                                placeholder="搜索文章..."
                                @keyup.enter="handleSearch"
                            />
                        </div>
                    </div>

                    <button class="lg:hidden action-btn" @click="toggleMobileMenu">
                        <svg v-if="!isMobileMenuOpen" class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                        </svg>
                        <svg v-else class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                        </svg>
                    </button>

                    <template v-if="!isLogin">
                        <button class="btn btn-primary" @click="goToLogin">
                            登录
                        </button>
                    </template>
                    <template v-else>
                        <button v-if="canPublish" class="btn btn-primary" @click="goToCreateArticle">
                            <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
                                <path d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"/>
                            </svg>
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

            <Transition name="slide">
                <MobileMenu
                    v-if="isMobileMenuOpen"
                    v-model="searchKeyword"
                    @search="handleSearch"
                    @close="closeMobileMenu"
                />
            </Transition>
        </nav>

        <Transition name="fade">
            <div
                v-if="isLogoutModalOpen"
                class="modal-overlay"
                aria-modal="true"
                role="dialog"
                @click="closeLogoutModal"
            >
                <div class="modal-content" role="document" @click.stop>
                    <div class="modal-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11V6m0 8h.01M19 10a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                        </svg>
                    </div>
                    <h3 class="modal-title">是否退出登录?</h3>
                    <div class="modal-actions">
                        <button class="btn btn-primary" @click="handleLogout">确认</button>
                        <button class="btn btn-outline" @click="closeLogoutModal">取消</button>
                    </div>
                </div>
            </div>
        </Transition>
    </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showMessage } from '@/utils'
import { useMainStore } from '@/stores'
import UserMenu from '@/components/layout/UserMenu.vue'
import MobileMenu from '@/components/layout/MobileMenu.vue'

const store = useMainStore()
const router = useRouter()
const route = useRoute()

const defaultLogo = new URL('@/assets/头像.jpg', import.meta.url).href

// 计算 Logo URL（优先使用用户头像，其次使用博客设置头像）
const logoUrl = computed(() => {
  // 优先使用用户头像
  const userAvatar = store.user?.avatar
  if (userAvatar) {
    if (userAvatar.startsWith('http://') || userAvatar.startsWith('https://')) {
      return userAvatar
    }
    if (userAvatar.startsWith('/')) {
      const baseApi = import.meta.env.VITE_APP_BASE_API.replace(/\/$/, '')
      return `${baseApi}${userAvatar}`
    }
    return `${import.meta.env.VITE_APP_BASE_API}/${userAvatar}`
  }

  // 其次使用博客设置头像
  const settingAvatar = store.setting?.avatar
  if (!settingAvatar) { return defaultLogo }
  if (settingAvatar.startsWith('http://') || settingAvatar.startsWith('https://')) {
    return settingAvatar
  }
  if (settingAvatar.startsWith('/')) {
    const baseApi = import.meta.env.VITE_APP_BASE_API.replace(/\/$/, '')
    return `${baseApi}${settingAvatar}`
  }
  return `${import.meta.env.VITE_APP_BASE_API}/${settingAvatar}`
})

const searchKeyword = ref('')
const isUserMenuOpen = ref(false)
const isLogoutModalOpen = ref(false)
const isMobileMenuOpen = ref(false)
const isScrolled = ref(false)
const scrollProgress = ref(0)

const navItems = [
    { name: '首页', path: '/' },
    { name: '发现', path: '/discover' },
    { name: '分类', path: '/category' },
    { name: '标签', path: '/tag' },
    { name: '归档', path: '/archive' }
]

const isLogin = computed(() => store.isLoggedIn())
const isAdmin = computed(() => store.user?.roles?.includes('ADMIN') || store.user?.roles?.includes('SUPER_ADMIN'))
const canPublish = computed(() => store.isLoggedIn())

const isActive = (path) => {
    if (path === '/') {
        return route.path === '/'
    }
    return route.path.startsWith(path)
}

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

const handleSearch = () => {
    if (!searchKeyword.value) {return}
    router.push({ path: '/search', query: { keyword: searchKeyword.value } })
    closeMobileMenu()
}

const handleLogout = () => {
    store.logout()
    isLogoutModalOpen.value = false
    showMessage('退出登录成功', 'success')
}

const goToLogin = () => {
    router.push('/login')
}

const goToCreateArticle = () => {
    router.push('/user/articles/create')
}

const handleClickOutside = (event) => {
    if (!event.target.closest('#user-menu-button')) {
        isUserMenuOpen.value = false
    }
}

const handleScroll = () => {
    isScrolled.value = window.scrollY > 50

    const winHeight = window.innerHeight
    const docHeight = document.documentElement.scrollHeight - winHeight
    scrollProgress.value = docHeight > 0 ? (window.scrollY / docHeight) * 100 : 0
}

onMounted(() => {
    document.addEventListener('click', handleClickOutside)
    window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside)
    window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.header-glass {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 1000;
    background: var(--glass-bg);
    backdrop-filter: blur(var(--glass-blur-xl));
    -webkit-backdrop-filter: blur(var(--glass-blur-xl));
    border-bottom: 1px solid var(--glass-border);
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.scroll-progress {
    position: absolute;
    top: 0;
    left: 0;
    height: 3px;
    background: var(--gradient-1);
    transition: width 0.1s linear;
    z-index: 1001;
}

.header-glass.scrolled {
    background: var(--glass-bg-hover);
    box-shadow: var(--shadow-md);
}

.header-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 72px;
    transition: height var(--transition-normal);
}

.scrolled .header-content {
    height: 64px;
}

.logo-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    text-decoration: none;
    transition: transform var(--transition-fast);
}

.logo-wrapper:hover {
    transform: scale(1.02);
}

.logo-image {
    width: 40px;
    height: 40px;
    border-radius: var(--radius-lg);
    object-fit: cover;
    box-shadow: var(--shadow-sm);
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.scrolled .logo-image {
    width: 36px;
    height: 36px;
}

.logo-text {
    font-size: 20px;
    font-weight: 700;
    background: var(--gradient-1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    transition: font-size var(--transition-fast);
}

.scrolled .logo-text {
    font-size: 18px;
}

.nav-links {
    display: none;
    list-style: none;
    gap: 4px;
    margin: 0;
    padding: 0;
}

@media (min-width: 1024px) {
    .nav-links {
        display: flex;
    }
}

.nav-link {
    position: relative;
    display: block;
    padding: 8px 16px;
    color: var(--text-secondary);
    font-weight: 500;
    font-size: 15px;
    text-decoration: none;
    border-radius: var(--radius-md);
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    overflow: hidden;
}

.nav-link::before {
    content: '';
    position: absolute;
    inset: 0;
    background: var(--gradient-1);
    opacity: 0;
    transition: opacity var(--transition-fast);
}

.nav-link:hover {
    color: white;
}

.nav-link:hover::before {
    opacity: 1;
}

.nav-link-text {
    position: relative;
    z-index: 1;
}

.nav-link-active {
    color: var(--color-primary);
}

.nav-link-active::before {
    opacity: 1;
    background: var(--color-primary-subtle);
}

.nav-link-active:hover {
    color: var(--color-primary);
}

.nav-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border: none;
    border-radius: var(--radius-lg);
    background: transparent;
    color: var(--text-secondary);
    cursor: pointer;
    transition: background-color 0.2s ease, color 0.2s ease;
}

.action-btn:hover {
    background: var(--color-primary-subtle);
    color: var(--color-primary);
}

.action-btn:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
}

.action-icon {
    width: 24px;
    height: 24px;
}

.search-wrapper {
    margin: 0 8px;
}

.search-bar {
    position: relative;
    display: flex;
    align-items: center;
}

.search-icon {
    position: absolute;
    left: 12px;
    width: 16px;
    height: 16px;
    color: var(--text-muted);
    pointer-events: none;
}

.search-input {
    width: 200px;
    max-width: calc(100vw - 120px);
    padding: 10px 12px 10px 36px;
    background: var(--bg-secondary);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-full);
    color: var(--text-primary);
    font-size: 14px;
    transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-input::placeholder {
    color: var(--text-muted);
}

.search-input:focus {
    outline: none;
    width: 260px;
    max-width: calc(100vw - 80px);
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px var(--color-primary-subtle);
}

.btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 10px 18px;
    border-radius: var(--radius-lg);
    font-weight: 600;
    font-size: 14px;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    text-decoration: none;
    border: none;
}

.btn-icon {
    width: 16px;
    height: 16px;
}

.btn-primary {
    background: var(--gradient-1);
    color: white;
    box-shadow: var(--shadow-primary);
}

.btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.5);
}

.btn-outline {
    background: transparent;
    border: 1px solid var(--border-color);
    color: var(--text-secondary);
}

.btn-outline:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
    background: var(--color-primary-subtle);
}

.modal-overlay {
    position: fixed;
    inset: 0;
    z-index: 2000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: rgba(10, 10, 20, 0.6);
    backdrop-filter: blur(4px);
}

.modal-content {
    width: 100%;
    max-width: 400px;
    padding: 32px;
    background: var(--bg-primary);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-xl);
    text-align: center;
}

.modal-icon {
    width: 64px;
    height: 64px;
    margin: 0 auto 20px;
    color: var(--color-warning);
}

.modal-icon svg {
    width: 100%;
    height: 100%;
}

.modal-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 24px;
}

.modal-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
}

.slide-enter-active,
.slide-leave-active {
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.slide-enter-from,
.slide-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

@media (max-width: 640px) {
    .search-wrapper {
        display: none;
    }
}

@media (prefers-reduced-motion: reduce) {
    .header-glass,
    .header-content,
    .logo-wrapper,
    .logo-image,
    .logo-text,
    .nav-link,
    .action-btn,
    .btn,
    .search-input {
        transition: none;
    }

    .btn-primary:hover {
        transform: none;
    }

    .scroll-progress {
        transition: none;
    }
}
</style>
