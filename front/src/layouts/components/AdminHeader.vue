<template>
    <el-affix :offset="0">
        <div class="header flex text-light-50 top-0 right-0 left-0 items-center">
            <!-- 展开、收缩侧边栏 -->
            <el-icon class="icon-btn" @click="store.handleMenuWidth()">
                <Fold v-if="store.menuWidth == '250px'" />
                <Expand v-else />
            </el-icon>

            <!-- 面包屑导航 -->
            <div class="breadcrumb ml-4 hidden md:block">
                <el-breadcrumb separator="/" class="text-sm">
                    <el-breadcrumb-item :to="{ path: '/admin' }">
                        <el-icon class="mr-1"><House /></el-icon>
                        <span>仪表盘</span>
                    </el-breadcrumb-item>
                    <el-breadcrumb-item v-for="item in breadcrumbItems" :key="item.name" :to="item.path || undefined">
                        {{ item.name }}
                    </el-breadcrumb-item>
                </el-breadcrumb>
            </div>

            <div class="ml-auto flex justify-center items-center">
                <!-- 全局搜索 -->
                <div class="search-box mr-4 hidden lg:block">
                    <el-input
                        v-model="searchQuery"
                        placeholder="搜索..."
                        :prefix-icon="Search"
                        clearable
                        class="search-input"
                        @keyup.enter="handleSearch"
                    />
                </div>

                <!-- 通知中心 -->
                <el-tooltip class="box-item" effect="dark" content="通知中心" placement="bottom">
                    <el-badge value="3" :hidden="notificationCount === 0" class="notification-badge">
                        <el-icon class="icon-btn" @click="showNotifications">
                            <Bell />
                        </el-icon>
                    </el-badge>
                </el-tooltip>

                <!-- 跳转博客前台 -->
                <el-tooltip class="box-item" effect="dark" content="跳转博客前台" placement="bottom">
                    <el-icon class="icon-btn" @click="router.push('/')">
                        <House />
                    </el-icon>
                </el-tooltip>

                <!-- 刷新 -->
                <el-tooltip class="box-item" effect="dark" content="刷新" placement="bottom">
                    <el-icon class="icon-btn" @click="refresh">
                        <Refresh />
                    </el-icon>
                </el-tooltip>

                <!-- 全屏 -->
                <el-tooltip class="box-item" effect="dark" content="全屏" placement="bottom">
                    <el-icon class="icon-btn" @click="toggle">
                        <FullScreen v-if="!isFullscreen" />
                        <Aim v-else />
                    </el-icon>
                </el-tooltip>

                <el-dropdown class="dropdown flex justify-center items-center text-light-50 mx-5" @command="handleCommand">
                    <span class="flex justify-center items-center">
                        <el-avatar :size="25" :src="store.user.avatar" class="mr-2" />
                        {{ store.user.username }}
                        <el-icon class="el-icon--right">
                            <arrow-down />
                        </el-icon>
                    </span>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                            <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                            <el-dropdown-item command="updatePassword">修改密码</el-dropdown-item>
                            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>
    </el-affix>

    <!-- 通知中心弹窗 -->
    <el-dialog v-model="notificationDialog" title="通知中心" width="350px" :show-close="true">
        <div class="notification-list">
            <div class="notification-item p-3 border-b border-gray-200 hover:bg-gray-50 transition-colors">
                <div class="flex justify-between items-start">
                    <div class="font-medium text-sm">新评论</div>
                    <div class="text-xs text-gray-500">5分钟前</div>
                </div>
                <div class="text-sm text-gray-600 mt-1">用户 "访客" 评论了文章 "Vue 3 教程"</div>
            </div>
            <div class="notification-item p-3 border-b border-gray-200 hover:bg-gray-50 transition-colors">
                <div class="flex justify-between items-start">
                    <div class="font-medium text-sm">系统更新</div>
                    <div class="text-xs text-gray-500">2小时前</div>
                </div>
                <div class="text-sm text-gray-600 mt-1">系统已更新至最新版本 v1.2.0</div>
            </div>
            <div class="notification-item p-3 border-b border-gray-200 hover:bg-gray-50 transition-colors">
                <div class="flex justify-between items-start">
                    <div class="font-medium text-sm">安全提醒</div>
                    <div class="text-xs text-gray-500">昨天</div>
                </div>
                <div class="text-sm text-gray-600 mt-1">检测到异常登录尝试，已自动阻止</div>
            </div>
        </div>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="notificationDialog = false">关闭</el-button>
                <el-button type="primary" @click="markAllAsRead">全部已读</el-button>
            </span>
        </template>
    </el-dialog>

    <!-- 修改密码 -->
    <el-dialog v-model="dialogShow" title="修改密码" width="30%" :show-close="false">
        <el-form ref="formRef" :rules="rules" :model="form">
            <el-form-item label="用户名" prop="oldPassword" label-width="120px">
                <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword" label-width="120px">
                <el-input
v-model="form.newPassword" type="password" autocomplete="off" placeholder="请输入新密码"
                    show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="rePassword" label-width="120px">
                <el-input
v-model="form.rePassword" type="password" autocomplete="off" placeholder="请再次确认新密码"
                    show-password />
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="dialogShow = false">取消</el-button>
                <el-button type="primary" @click="onSubmit">
                    提交
                </el-button>
            </span>
        </template>
    </el-dialog>
</template>

<script setup>
import { showModel, showMessage } from '@/utils'
import { useRouter, useRoute } from 'vue-router'
import { ref, reactive, computed } from 'vue'
import { useFullscreen } from '@vueuse/core'
import { updateAdminPassword } from '@/api/admin/user'
import { useMainStore } from '@/stores'
import { Search, Bell } from '@element-plus/icons-vue'
import { API_STATUS } from '@/composables/api'

const { isFullscreen, toggle } = useFullscreen()

const store = useMainStore()
const router = useRouter()
const route = useRoute()

// 面包屑导航
const breadcrumbItems = computed(() => {
    const items = []
    const currentRoute = route
    
    if (currentRoute.name) {
        // 根据路由名称生成面包屑
        switch (currentRoute.name) {
            case 'admin-article-list':
                items.push({ name: '文章管理' })
                break
            case 'admin-category-list':
                items.push({ name: '分类管理' })
                break
            case 'admin-tag-list':
                items.push({ name: '标签管理' })
                break
            case 'admin-comments':
                items.push({ name: '评论管理' })
                break
            case 'admin-users':
                items.push({ name: '用户管理' })
                break
            case 'admin-media':
                items.push({ name: '媒体库' })
                break
            case 'admin-blog-setting':
                items.push({ name: '博客设置' })
                break
            case 'admin-analytics':
                items.push({ name: '数据分析' })
                if (currentRoute.path.includes('page-views')) {
                    items.push({ name: '页面访问' })
                }
                break
        }
    }
    return items
})

// 搜索功能
const searchQuery = ref('')
const handleSearch = () => {
    if (searchQuery.value.trim()) {
        showMessage(`搜索: ${searchQuery.value}`, 'info')
        // 这里可以添加实际的搜索逻辑
    }
}

// 通知中心
const notificationDialog = ref(false)
const notificationCount = ref(3)

const showNotifications = () => {
    notificationDialog.value = true
}

const markAllAsRead = () => {
    notificationCount.value = 0
    showMessage('所有通知已标记为已读', 'success')
}

// 修改密码
const dialogShow = ref(false)

const form = reactive({
    username: store.user.username,
    newPassword: '',
    rePassword: ''
})

const rules = {
    username: [
        {
            required: true,
            message: '用户名不能为空',
            trigger: 'blur'
        }
    ],
    newPassword: [
        {
            required: true,
            message: '新密码不能为空',
            trigger: 'blur'
        }
    ],
    rePassword: [
        {
            required: true,
            message: '请再次输入密码',
            trigger: 'blur'
        }
    ]
}

const formRef = ref(null)

const onSubmit = () => {
    // 登录表单验证
    formRef.value.validate((valid) => {
        if (!valid) {
            return false
        }

        // 校验两次输入的密码是否一致
        if (form.newPassword !== form.rePassword) {
            showMessage('两次输入的密码不一致！', 'warning')
            return
        }

        updateAdminPassword(form).then(res => {
            if (res.code === API_STATUS.SUCCESS) {
                // 提示
                showMessage('重置密码成功, 请重新登录！', 'success', 'message')
                store.logout()
                router.push('/login')
                dialogShow.value = false
            } else {
                const message = res.message
                showMessage(message, 'warning')
            }
        })
    })
}

const handleCommand = (e) => {
    switch (e) {
        case 'profile':
            showMessage('个人资料功能开发中', 'info')
            break
        case 'settings':
            router.push('/admin/settings/site')
            break
        case 'updatePassword':
            dialogShow.value = true
            break
        case 'logout':
            logout()
            break
    }
}

// 刷新页面
const refresh = () => location.reload()

function logout() {
    showModel('是否确定要退出登录？').then(() => {
        store.logout()

        // 跳转回登录页
        router.push('/login')

        // 提示登出成功
        showMessage('退出登录成功', 'success')
    }).catch(() => { })
}
</script>

<style>
.header {
    height: 64px;
    background-color: var(--bg-primary, #ffffff);
    z-index: 100;
    box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.08));
}

.icon-btn {
    @apply flex justify-center items-center;
    width: 42px;
    height: 64px;
    cursor: pointer;
    color: var(--text-secondary, #374151);
    transition: background-color $1, border-color $1, color $1, box-shadow $1, transform $1$2
}

.icon-btn:hover {
    @apply bg-gray-100;
    color: var(--color-primary, #6366F1);
}

.header .dropdown {
    height: 64px;
    cursor: pointer;
    color: var(--text-secondary, #374151) !important;
    transition: background-color $1, border-color $1, color $1, box-shadow $1, transform $1$2
}

.header .dropdown:hover {
    @apply bg-gray-100;
}

.search-input {
    width: 200px;
    transition: background-color $1, border-color $1, color $1, box-shadow $1, transform $1$2
    border-radius: var(--radius-full, 9999px);
    height: 36px;
}

.search-input:hover {
    width: 240px;
}

.search-input:focus {
    width: 280px;
    box-shadow: var(--focus-outline) !important;
}

.notification-badge {
    position: relative;
}

.notification-badge .el-badge__content {
    background-color: var(--color-error, #F87171);
    color: white;
    font-size: 10px;
    min-width: 18px;
    height: 18px;
    line-height: 18px;
    border-radius: var(--radius-full, 9999px);
    box-shadow: 0 1px 3px rgba(239, 68, 68, 0.4);
}

.breadcrumb {
    height: 64px;
    display: flex;
    align-items: center;
}

.breadcrumb .el-breadcrumb {
    font-size: 14px;
}

.breadcrumb .el-breadcrumb__item:last-child .el-breadcrumb__inner {
    color: var(--color-primary, #6366F1);
    font-weight: 500;
}

.breadcrumb .el-breadcrumb__inner a:hover {
    color: var(--color-primary-hover, #818CF8);
}

@media (max-width: 1024px) {
    .search-box {
        display: none !important;
    }
}

@media (max-width: 768px) {
    .breadcrumb {
        display: none !important;
    }
}

.notification-list {
    max-height: 300px;
    overflow-y: auto;
}

.notification-item {
    transition: background-color $1, border-color $1, color $1, box-shadow $1, transform $1$2
}

.notification-item:hover {
    background-color: var(--bg-tertiary, #f3f4f6) !important;
}

.dark .header {
    background-color: var(--bg-card, #16161A);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.dark .icon-btn {
    color: var(--text-primary, #F3F4F6);
}

.dark .icon-btn:hover {
    @apply bg-gray-700;
    color: var(--color-accent-hover, #93C5FD);
}

.dark .header .dropdown {
    color: var(--text-primary, #F3F4F6) !important;
}

.dark .header .dropdown:hover {
    @apply bg-gray-700;
}

.dark .search-input {
    background-color: var(--bg-elevated, #1A1A1D);
    border-color: var(--border-color, #27272A);
    color: var(--text-primary, #F3F4F6);
}

.dark .search-input::placeholder {
    color: var(--text-muted, #71717A);
}

.dark .breadcrumb .el-breadcrumb__item .el-breadcrumb__inner {
    color: var(--text-primary, #F3F4F6);
}

.dark .breadcrumb .el-breadcrumb__item:last-child .el-breadcrumb__inner {
    color: var(--color-accent-hover, #93C5FD);
}

.dark .breadcrumb .el-breadcrumb__inner a:hover {
    color: var(--color-accent-hover, #93C5FD);
}

.dark .notification-item {
    border-color: var(--border-color, #27272A);
}

.dark .notification-item:hover {
    background-color: var(--bg-tertiary, #16161A) !important;
}

.dark .notification-item .text-gray-600 {
    color: var(--text-secondary, #E5E7EB);
}

.dark .notification-item .text-gray-500 {
    color: var(--text-muted, #71717A);
}
</style>
