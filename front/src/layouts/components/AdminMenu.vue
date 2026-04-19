<template>
    <div class="menu shadow-md fixed transition-all duration-300" :class="{ 'collapsed': isCollapse, 'mobile-open': isMobileOpen }" :style="{ width: isMobile ? '280px' : store.menuWidth }">
        <!-- 侧边栏头部 -->
        <div class="sidebar-header flex items-center justify-between px-4 h-[64px] border-b border-gray-700/30">
            <div v-if="!isCollapse || isMobile" class="flex items-center space-x-3">
                <div class="sidebar-logo w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center">
                    <el-icon class="text-white"><Monitor /></el-icon>
                </div>
                <span class="sidebar-title text-white font-semibold text-lg">Admin Panel</span>
            </div>
            <div v-else class="flex items-center justify-center w-full h-full">
                <div class="sidebar-logo-collapsed w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center">
                    <el-icon class="text-white"><Monitor /></el-icon>
                </div>
            </div>

            <!-- 折叠/展开按钮 -->
            <button
                v-if="!isMobile"
                class="collapse-btn ml-2 p-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-gray-700/50 transition-all duration-200"
                :title="isCollapse ? '展开' : '折叠'"
                @click="toggleCollapse"
            >
                <el-icon class="transition-transform duration-300" :class="{ 'rotate-180': isCollapse }">
                    <Fold v-if="!isCollapse" />
                    <Expand v-else />
                </el-icon>
            </button>

            <!-- 移动端关闭按钮 -->
            <button
                v-if="isMobile"
                class="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-gray-700/50 transition-all duration-200"
                @click="closeMobileMenu"
            >
                <el-icon><Close /></el-icon>
            </button>
        </div>

        <!-- 菜单搜索框 -->
        <div class="menu-search px-3 py-2">
            <div class="relative">
                <el-icon class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
                    <Search />
                </el-icon>
                <input
                    v-model="searchKeyword"
                    type="text"
                    placeholder="搜索菜单..."
                    class="w-full pl-9 pr-3 py-2 text-sm bg-gray-800/50 border border-gray-700 rounded-lg text-gray-300 placeholder-gray-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30 transition-all duration-200"
                >
            </div>
        </div>

        <el-menu
            :collapse="isCollapse && !isMobile"
            class="border-0 admin-el-menu"
            :default-active="defaultActive"
            :collapse-transition="true"
            unique-opened
            router
            @select="handleSelect"
        >
            <template v-for="item in filteredMenus" :key="item.path">
                <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
                    <template #title>
                        <el-icon class="menu-icon">
                            <component :is="item.icon" v-if="item.icon" />
                            <Menu v-else />
                        </el-icon>
                        <span class="menu-text">{{ item.name }}</span>
                    </template>
                    <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path" class="sub-menu-item">
                        <span class="sub-menu-text">{{ child.name }}</span>
                    </el-menu-item>
                </el-sub-menu>
                <el-menu-item v-else :index="item.path" class="admin-el-menu-item">
                    <el-icon class="menu-icon">
                        <component :is="item.icon" v-if="item.icon" />
                        <Menu v-else />
                    </el-icon>
                    <span class="menu-text">{{ item.name }}</span>
                </el-menu-item>
            </template>
        </el-menu>

        <!-- 空状态 -->
        <div v-if="filteredMenus.length === 0" class="text-center py-8 text-gray-500">
            <el-icon class="text-2xl mb-2"><Search /></el-icon>
            <p class="text-sm">未找到匹配的菜单</p>
        </div>
    </div>

    <!-- 移动端遮罩层 -->
    <div
        v-if="isMobile && isMobileOpen"
        class="mobile-overlay fixed inset-0 bg-black/50 z-[999]"
        @click="closeMobileMenu"
    ></div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useMainStore, useAuthStore } from '@/stores'
import {
    Monitor, Document, FolderOpened, PriceTag, ChatLineRound,
    User, Setting, DataAnalysis, Picture, Menu, Fold, Expand,
    Close, Search
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const store = useMainStore()
const authStore = useAuthStore()

const defaultActive = ref(route.path)
const searchKeyword = ref('')
const isMobile = ref(false)
const isMobileOpen = ref(false)

// 监听路由变化，更新默认激活项
watch(() => route.path, (newPath) => {
    defaultActive.value = newPath
})

// 是否折叠 - 从 localStorage 读取
const isCollapse = computed(() => {
    if (isMobile.value) {return false}
    return !(store.menuWidth === '250px')
})

// 切换折叠状态
const toggleCollapse = () => {
    store.handleMenuWidth()
}

// 打开移动端菜单
const openMobileMenu = () => {
    isMobileOpen.value = true
    document.body.style.overflow = 'hidden'
}

// 关闭移动端菜单
const closeMobileMenu = () => {
    isMobileOpen.value = false
    document.body.style.overflow = ''
}

// 检测是否为移动端
const checkMobile = () => {
    isMobile.value = window.innerWidth <= 768
    if (!isMobile.value) {
        closeMobileMenu()
    }
}

onMounted(() => {
    checkMobile()
    window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
    window.removeEventListener('resize', checkMobile)
})

// 暴露方法给父组件
defineExpose({
    openMobileMenu,
    closeMobileMenu
})

// 菜单配置 - 支持权限控制
const menuConfig = [
    {
        name: '仪表盘',
        icon: Monitor,
        path: '/admin',
        roles: ['admin', 'editor'],
        children: []
    },
    {
        name: '文章管理',
        icon: Document,
        path: '/admin/articles',
        roles: ['admin', 'editor'],
        children: [
            { name: '文章列表', path: '/admin/articles', roles: ['admin', 'editor'] },
            { name: '发布文章', path: '/admin/articles/create', roles: ['admin', 'editor'] }
        ]
    },
    {
        name: '分类管理',
        icon: FolderOpened,
        path: '/admin/categories',
        roles: ['admin', 'editor'],
        children: [
            { name: '分类列表', path: '/admin/categories', roles: ['admin', 'editor'] },
            { name: '添加分类', path: '/admin/categories/create', roles: ['admin'] }
        ]
    },
    {
        name: '标签管理',
        icon: PriceTag,
        path: '/admin/tags',
        roles: ['admin', 'editor'],
        children: [
            { name: '标签列表', path: '/admin/tags', roles: ['admin', 'editor'] },
            { name: '添加标签', path: '/admin/tags/create', roles: ['admin'] }
        ]
    },
    {
        name: '评论管理',
        icon: ChatLineRound,
        path: '/admin/comments',
        roles: ['admin', 'editor'],
        children: []
    },
    {
        name: '用户管理',
        icon: User,
        path: '/admin/users',
        roles: ['admin'],
        children: []
    },
    {
        name: '系统设置',
        icon: Setting,
        path: '/admin/blog-setting',
        roles: ['admin'],
        children: [
            { name: '博客设置', path: '/admin/blog-setting', roles: ['admin'] }
        ]
    },
    {
        name: '数据分析',
        icon: DataAnalysis,
        path: '/admin/analytics/page-views',
        roles: ['admin'],
        children: [
            { name: '页面访问', path: '/admin/analytics/page-views', roles: ['admin'] }
        ]
    },
    {
        name: '媒体库',
        icon: Picture,
        path: '/admin/media',
        roles: ['admin', 'editor'],
        children: [
            { name: '媒体库', path: '/admin/media', roles: ['admin', 'editor'] }
        ]
    }
]

// 获取当前用户角色
const userRoles = computed(() => {
    return authStore.user?.roles || ['admin']
})

// 检查是否有权限
const hasPermission = (roles) => {
    if (!roles || roles.length === 0) {return true}
    return roles.some(role => userRoles.value.includes(role))
}

// 过滤有权限的菜单
const menus = computed(() => {
    return menuConfig.filter(item => {
        if (!hasPermission(item.roles)) {return false}
        // 过滤子菜单
        if (item.children && item.children.length > 0) {
            item.children = item.children.filter(child => hasPermission(child.roles))
        }
        return true
    })
})

// 搜索过滤后的菜单
const filteredMenus = computed(() => {
    if (!searchKeyword.value.trim()) {return menus.value}

    const keyword = searchKeyword.value.toLowerCase()
    return menus.value.filter(item => {
        // 匹配父菜单
        if (item.name.toLowerCase().includes(keyword)) {return true}

        // 匹配子菜单
        if (item.children && item.children.length > 0) {
            const matchedChildren = item.children.filter(child =>
                child.name.toLowerCase().includes(keyword)
            )
            return matchedChildren.length > 0
        }
        return false
    })
})

const handleSelect = (key) => {
    router.push(key)
    if (isMobile.value) {
        closeMobileMenu()
    }
}
</script>

<style scoped>
.menu {
    transition: all 0.3s ease;
    width: 250px;
    top: 0;
    bottom: 0;
    left: 0;
    overflow-y: auto;
    overflow-x: hidden;
    background-color: #0f172a;
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
    z-index: 1000;
}

/* 折叠按钮 */
.collapse-btn {
    opacity: 0.7;
}

.collapse-btn:hover {
    opacity: 1;
}

/* 菜单搜索 */
.menu-search {
    border-bottom: 1px solid rgba(75, 85, 99, 0.3);
}

/* 侧边栏头部样式 */
.sidebar-header {
    background-color: #1e293b;
    transition: all 0.3s ease;
}

.sidebar-logo {
    transition: all 0.3s ease;
}

.sidebar-logo:hover {
    transform: scale(1.05);
}

.sidebar-title {
    transition: all 0.3s ease;
}

.sidebar-logo-collapsed {
    transition: all 0.3s ease;
}

.sidebar-logo-collapsed:hover {
    transform: scale(1.05);
}

/* 菜单样式 */
.admin-el-menu {
    background-color: #0f172a;
    border-right: 0;
    padding: 8px 0;
}

/* 菜单图标 */
.menu-icon {
    font-size: 18px;
    transition: all 0.2s ease;
}

/* 菜单项 */
.admin-el-menu-item,
:deep(.el-sub-menu__title) {
    color: #94a3b8;
    height: 48px;
    line-height: 48px;
    margin: 4px 8px;
    border-radius: 8px;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
}

/* 菜单项文本 */
.menu-text {
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s ease;
}

/* 菜单项悬停 */
.admin-el-menu-item:hover,
:deep(.el-sub-menu__title:hover) {
    background-color: rgba(59, 130, 246, 0.1);
    color: #60a5fa;
}

.admin-el-menu-item:hover .menu-icon,
:deep(.el-sub-menu__title:hover .menu-icon) {
    color: #60a5fa;
    transform: translateX(4px);
}

.admin-el-menu-item:hover .menu-text,
:deep(.el-sub-menu__title:hover .menu-text) {
    color: #60a5fa;
}

/* 菜单项激活状态 */
:deep(.el-menu-item.is-active) {
    background-color: rgba(59, 130, 246, 0.15);
    color: #3b82f6;
}

:deep(.el-menu-item.is-active::before) {
    content: "";
    position: absolute;
    top: 50%;
    left: 0;
    transform: translateY(-50%);
    width: 3px;
    height: 60%;
    background-color: #3b82f6;
    border-radius: 0 3px 3px 0;
}

:deep(.el-menu-item.is-active .menu-icon) {
    color: #3b82f6;
    transform: translateX(4px);
}

:deep(.el-menu-item.is-active .menu-text) {
    color: #3b82f6;
    font-weight: 600;
}

/* 子菜单项 */
.sub-menu-item {
    color: #94a3b8;
    height: 40px;
    line-height: 40px;
    padding-left: 48px;
    margin: 2px 8px;
    border-radius: 6px;
    transition: all 0.3s ease;
}

.sub-menu-text {
    font-size: 13px;
    transition: all 0.2s ease;
}

.sub-menu-item:hover {
    background-color: rgba(59, 130, 246, 0.1);
    color: #60a5fa;
    padding-left: 52px;
}

.sub-menu-item:hover .sub-menu-text {
    color: #60a5fa;
}

.sub-menu-item.is-active {
    background-color: rgba(59, 130, 246, 0.15);
    color: #3b82f6;
    padding-left: 52px;
}

.sub-menu-item.is-active .sub-menu-text {
    color: #3b82f6;
    font-weight: 600;
}

/* 子菜单展开/折叠动画 */
:deep(.el-sub-menu .el-menu) {
    background-color: #0f172a;
    padding: 4px 0;
}

:deep(.el-sub-menu__title) {
    border-radius: 8px;
    margin: 4px 8px;
}

:deep(.el-sub-menu__title .el-icon) {
    transition: all 0.3s ease;
}

:deep(.el-sub-menu.is-opened .el-sub-menu__title) {
    background-color: rgba(59, 130, 246, 0.1);
    color: #60a5fa;
}

:deep(.el-sub-menu.is-opened .el-sub-menu__title .menu-icon) {
    color: #60a5fa;
    transform: translateX(4px);
}

:deep(.el-sub-menu.is-opened .el-sub-menu__title .menu-text) {
    color: #60a5fa;
}

/* 滚动条样式 */
.menu::-webkit-scrollbar {
    width: 6px;
}

.menu::-webkit-scrollbar-track {
    background-color: #1e293b;
    border-radius: 3px;
}

.menu::-webkit-scrollbar-thumb {
    background-color: #475569;
    border-radius: 3px;
    transition: all 0.2s ease;
}

.menu::-webkit-scrollbar-thumb:hover {
    background-color: #64748b;
}

/* 响应式调整 - 移动端 */
@media (max-width: 768px) {
    .menu {
        transform: translateX(-100%);
        width: 280px;
    }

    .menu.mobile-open {
        transform: translateX(0);
    }

    .mobile-overlay {
        backdrop-filter: blur(2px);
    }
}

/* 深色模式适配 */
.dark .menu {
    background-color: #0f172a;
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.2);
}

.dark .sidebar-header {
    background-color: #1e293b;
}

.dark .admin-el-menu {
    background-color: #0f172a;
}

.dark :deep(.el-sub-menu .el-menu) {
    background-color: #0f172a;
}

.dark .menu::-webkit-scrollbar-track {
    background-color: #1e293b;
}

.dark .menu::-webkit-scrollbar-thumb {
    background-color: #475569;
}

.dark .menu::-webkit-scrollbar-thumb:hover {
    background-color: #64748b;
}
</style>
