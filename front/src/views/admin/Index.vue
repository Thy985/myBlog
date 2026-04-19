<template>
    <div class="dashboard-container">
        <!-- 页面标题 -->
        <div class="page-header mb-6 flex flex-col md:flex-row md:justify-between md:items-center">
            <div>
                <h1 class="text-2xl font-bold text-gray-800">仪表盘</h1>
                <p class="text-gray-500">欢迎回来，{{ userName }}！这里是您的系统概览。</p>
            </div>
            <div class="mt-4 md:mt-0 flex items-center gap-4">
                <el-button type="primary" icon="Refresh" class="flex items-center gap-2" @click="refreshData">
                    刷新数据
                </el-button>
                <el-dropdown>
                    <el-button type="default" icon="More"></el-button>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item @click="exportData">导出数据</el-dropdown-item>
                            <el-dropdown-item @click="resetDashboard">重置布局</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>

        <!-- 快捷操作 -->
        <DashboardQuickActions />

        <!-- 系统概览卡片 -->
        <DashboardStats :stats="stats" />

        <!-- 图表区域 -->
        <DashboardCharts ref="dashboardCharts" :time-range="chartTimeRange" @change="updateCharts" />

        <!-- 最近动态 -->
        <DashboardActivity
            :recent-articles="recentArticles"
            :recent-comments="recentComments"
        />

        <!-- 系统状态 -->
        <DashboardSystemStatus :current-date-time="currentDateTime" />
    </div>
</template>

<script setup>
import DashboardQuickActions from '@/components/admin/DashboardQuickActions.vue'
import DashboardStats from '@/components/admin/DashboardStats.vue'
import DashboardCharts from '@/components/admin/DashboardCharts.vue'
import DashboardActivity from '@/components/admin/DashboardActivity.vue'
import DashboardSystemStatus from '@/components/admin/DashboardSystemStatus.vue'
import { ref, onMounted, onUnmounted } from 'vue'
import { getDashboardArticleStatisticsInfo } from '@/api/admin/dashboard'
import { showMessage } from '@/utils'
import { useMainStore } from '@/stores'

const store = useMainStore()

// 响应式数据
const articleTotalCount = ref(0)
const categoryTotalCount = ref(0)
const tagTotalCount = ref(0)
const pvTotalCount = ref(0)
const commentTotalCount = ref(0)
const userTotalCount = ref(0)
const chartTimeRange = ref('7')
const userName = ref('管理员')
const currentDateTime = ref('')
const isLoading = ref(false)

// 图表组件引用
const dashboardCharts = ref(null)

// 最近文章和评论数据
const recentArticles = ref([
    { title: 'Vue 3 Composition API 实践', createTime: '2024-01-01', viewCount: 123 },
    { title: 'TypeScript 进阶技巧', createTime: '2024-01-02', viewCount: 98 },
    { title: 'Tailwind CSS 最佳实践', createTime: '2024-01-03', viewCount: 234 },
    { title: '前端性能优化策略', createTime: '2024-01-04', viewCount: 156 },
    { title: 'React vs Vue 2024', createTime: '2024-01-05', viewCount: 345 }
])

const recentComments = ref([
    { content: '文章写得很棒！', createTime: '2024-01-01', userName: '用户1' },
    { content: '学习了很多，谢谢分享', createTime: '2024-01-02', userName: '用户2' },
    { content: '有个问题想请教一下', createTime: '2024-01-03', userName: '用户3' },
    { content: '期待更多优质内容', createTime: '2024-01-04', userName: '用户4' },
    { content: '非常实用的教程', createTime: '2024-01-05', userName: '用户5' }
])

// 统计数据配置
const stats = ref([
    {
        label: '文章',
        value: articleTotalCount,
        icon: 'Document',
        iconBg: 'bg-blue-100',
        iconColor: 'text-blue-500',
        change: 12
    },
    {
        label: '分类',
        value: categoryTotalCount,
        icon: 'FolderOpened',
        iconBg: 'bg-green-100',
        iconColor: 'text-green-500',
        change: 5
    },
    {
        label: '标签',
        value: tagTotalCount,
        icon: 'PriceTag',
        iconBg: 'bg-yellow-100',
        iconColor: 'text-yellow-500',
        change: 8
    },
    {
        label: '总浏览量',
        value: pvTotalCount,
        icon: 'View',
        iconBg: 'bg-purple-100',
        iconColor: 'text-purple-500',
        change: 25
    },
    {
        label: '评论',
        value: commentTotalCount,
        icon: 'ChatLineRound',
        iconBg: 'bg-red-100',
        iconColor: 'text-red-500',
        change: 15
    },
    {
        label: '用户',
        value: userTotalCount,
        icon: 'User',
        iconBg: 'bg-indigo-100',
        iconColor: 'text-indigo-500',
        change: 7
    }
])

// 刷新数据
const refreshData = () => {
    isLoading.value = true
    getDashboardStats().then(() => {
        isLoading.value = false
        showMessage('数据刷新成功', 'success')
    })
}

// 导出数据
const exportData = () => {
    showMessage('数据导出功能开发中', 'info')
}

// 重置仪表盘
const resetDashboard = () => {
    showMessage('仪表盘重置成功', 'success')
}

// 更新图表
const updateCharts = () => {
    if (dashboardCharts.value) {
        dashboardCharts.value.updateCharts()
    }
}

// 获取仪表盘统计数据
const getDashboardStats = () => {
    return new Promise((resolve, reject) => {
        getDashboardArticleStatisticsInfo().then((e) => {
            if (e.code === 200) {
                articleTotalCount.value = e.data.articleTotalCount
                categoryTotalCount.value = e.data.categoryTotalCount
                tagTotalCount.value = e.data.tagTotalCount
                pvTotalCount.value = e.data.pvTotalCount
                commentTotalCount.value = e.data.commentTotalCount || 0
                userTotalCount.value = e.data.userTotalCount || 0
                resolve()
            } else {
                reject()
            }
        }).catch(() => {
            reject()
        })
    })
}

// 更新当前时间
const updateCurrentDateTime = () => {
    const now = new Date()
    currentDateTime.value = now.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    })
}

// 定时器 ID
let timeInterval, statsInterval

// 页面加载时获取数据
onMounted(() => {
    // 获取用户信息
    if (store.user && store.user.username) {
        userName.value = store.user.username
    }

    // 获取仪表盘数据
    getDashboardStats()

    // 更新时间
    updateCurrentDateTime()
    timeInterval = setInterval(updateCurrentDateTime, 1000)

    // 定时刷新数据
    statsInterval = setInterval(getDashboardStats, 60000) // 每分钟刷新一次
})

// 组件卸载时清理定时器
onUnmounted(() => {
    clearInterval(timeInterval)
    clearInterval(statsInterval)
})
</script>

<style scoped>
/* 仪表盘容器 */
.dashboard-container {
    padding: 20px;
    background-color: #f8fafc;
    min-height: calc(100vh - 80px);
}

/* 页面标题 */
.page-header {
    margin-bottom: 24px;
}

.page-header h1 {
    font-size: 24px;
    font-weight: 700;
    color: #1f2937;
    margin-bottom: 8px;
}

.page-header p {
    font-size: 14px;
    color: #6b7280;
}

/* 响应式调整 */
@media (max-width: 1024px) {
    .dashboard-container {
        padding: 16px;
    }
}

@media (max-width: 768px) {
    .page-header h1 {
        font-size: 20px;
    }
}

/* 深色模式适配 */
.dark .dashboard-container {
    background-color: #0f172a;
}

.dark .page-header h1 {
    color: #f8fafc;
}

.dark .page-header p {
    color: #94a3b8;
}
</style>
