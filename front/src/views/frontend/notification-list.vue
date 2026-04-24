<template>
    <div class="notification-page">
        <el-card shadow="never">
            <template #header>
                <div class="card-header">
                    <span class="title">我的通知</span>
                    <div class="actions">
                        <el-button 
                            size="small" 
                            :disabled="unreadCount === 0"
                            @click="handleMarkAllRead"
                        >
                            全部已读
                        </el-button>
                        <el-button 
                            size="small" 
                            @click="handleClearAll"
                        >
                            清空通知
                        </el-button>
                        <el-button 
                            size="small" 
                            @click="goToSettings"
                        >
                            通知设置
                        </el-button>
                    </div>
                </div>
            </template>

            <!-- 筛选标签 -->
            <div class="filter-tabs">
                <el-radio-group v-model="filterType" @change="handleFilterChange">
                    <el-radio-button label="all">全部</el-radio-button>
                    <el-radio-button label="unread">未读</el-radio-button>
                    <el-radio-button label="COMMENT">评论</el-radio-button>
                    <el-radio-button label="LIKE">点赞</el-radio-button>
                    <el-radio-button label="SYSTEM">系统</el-radio-button>
                </el-radio-group>
            </div>

            <!-- 通知列表 -->
            <div v-loading="loading" class="notification-list">
                <div v-if="notifications.length === 0" class="empty">
                    <el-empty description="暂无通知" />
                </div>

                <div v-else>
                    <div
                        v-for="item in notifications"
                        :key="item.id"
                        class="notification-item"
                        :class="{ 'unread': !item.is_read }"
                    >
                        <div class="item-icon">
                            <el-icon v-if="item.type === 'COMMENT'" :size="24" color="#409EFF">
                                <ChatDotRound />
                            </el-icon>
                            <el-icon v-else-if="item.type === 'LIKE'" :size="24" color="#F56C6C">
                                <Star />
                            </el-icon>
                            <el-icon v-else :size="24" color="#E6A23C">
                                <Bell />
                            </el-icon>
                        </div>

                        <div class="item-content" @click="handleItemClick(item)">
                            <div class="item-header">
                                <span class="item-title">{{ item.title }}</span>
                                <span class="item-time">{{ formatTime(item.created_time) }}</span>
                            </div>
                            <div v-if="item.content" class="item-text">{{ item.content }}</div>
                            <div v-if="item.sender_name" class="item-footer">
                                <span class="sender">来自: {{ item.sender_name }}</span>
                            </div>
                        </div>

                        <div class="item-actions">
                            <el-button
                                v-if="!item.is_read"
                                link
                                size="small"
                                @click="handleMarkRead(item)"
                            >
                                标记已读
                            </el-button>
                            <el-button
                                link
                                size="small"
                                type="danger"
                                @click="handleDelete(item.id)"
                            >
                                删除
                            </el-button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 分页 -->
            <div v-if="total > 0" class="pagination">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :total="total"
                    :page-sizes="[10, 20, 50, 100]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="handleSizeChange"
                    @current-change="handlePageChange"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, ChatDotRound, Star } from '@element-plus/icons-vue'
import {
    getNotificationList,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    clearAllNotifications
} from '@/api/notification'
import dayjs from '@/utils/dayjs'

import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'
const router = useRouter()

// 数据
const loading = ref(false)
const notifications = ref([])
const filterType = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const unreadCount = ref(0)

// 加载通知列表
const loadNotifications = async () => {
    try {
        loading.value = true
        const res = await getNotificationList(currentPage.value, pageSize.value)
        if (res.code === API_STATUS.SUCCESS) {
            notifications.value = res.data.list
            total.value = res.data.total
            unreadCount.value = notifications.value.filter(item => !item.is_read).length
        }
    } catch (error) {
        logger.error('加载通知失败:', error)
        ElMessage.error('加载通知失败')
    } finally {
        loading.value = false
    }
}

// 筛选变化
const handleFilterChange = () => {
    currentPage.value = 1
    loadNotifications()
}

// 页码变化
const handlePageChange = () => {
    loadNotifications()
}

// 每页数量变化
const handleSizeChange = () => {
    currentPage.value = 1
    loadNotifications()
}

// 点击通知项
const handleItemClick = async (item) => {
    // 标记为已读
    if (!item.is_read) {
        await handleMarkRead(item)
    }

    // 跳转到目标页面
    if (item.target_type === 'ARTICLE' && item.target_id) {
        router.push({
            path: '/article/detail',
            query: { articleId: item.target_id }
        })
    }
}

// 标记单个为已读
const handleMarkRead = async (item) => {
    try {
        const res = await markAsRead(item.id)
        if (res.code === API_STATUS.SUCCESS) {
            item.is_read = true
            unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
    } catch (error) {
        ElMessage.error('操作失败')
    }
}

// 全部标记为已读
const handleMarkAllRead = async () => {
    try {
        const res = await markAllAsRead()
        if (res.code === API_STATUS.SUCCESS) {
            notifications.value.forEach(item => {
                item.is_read = true
            })
            unreadCount.value = 0
            ElMessage.success('已全部标记为已读')
        }
    } catch (error) {
        ElMessage.error('操作失败')
    }
}

// 清空所有通知
const handleClearAll = async () => {
    try {
        await ElMessageBox.confirm('确定要清空所有通知吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const res = await clearAllNotifications()
        if (res.code === API_STATUS.SUCCESS) {
            notifications.value = []
            total.value = 0
            unreadCount.value = 0
            ElMessage.success('已清空所有通知')
        }
    } catch (error) {
        if (error !== 'cancel') {
            ElMessage.error('操作失败')
        }
    }
}

// 删除单个通知
const handleDelete = async (id) => {
    try {
        await ElMessageBox.confirm('确定要删除这条通知吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const res = await deleteNotification(id)
        if (res.code === API_STATUS.SUCCESS) {
            const index = notifications.value.findIndex(item => item.id === id)
            if (index > -1) {
                const item = notifications.value[index]
                if (!item.is_read) {
                    unreadCount.value = Math.max(0, unreadCount.value - 1)
                }
                notifications.value.splice(index, 1)
                total.value--
            }
            ElMessage.success('删除成功')
        }
    } catch (error) {
        if (error !== 'cancel') {
            ElMessage.error('删除失败')
        }
    }
}

// 跳转到设置页面
const goToSettings = () => {
    router.push('/user/notification-settings')
}

// 格式化时间
const formatTime = (time) => {
    return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 组件挂载
onMounted(() => {
    loadNotifications()
})
</script>

<style scoped>
.notification-page {
    padding: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.card-header .title {
    font-size: 18px;
    font-weight: 600;
}

.card-header .actions {
    display: flex;
    gap: 8px;
}

.filter-tabs {
    margin-bottom: 20px;
}

.notification-list {
    min-height: 400px;
}

.empty {
    padding: 60px 0;
}

.notification-item {
    display: flex;
    gap: 16px;
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
    transition: background-color 0.2s;
}

.notification-item:hover {
    background-color: #f5f7fa;
}

.notification-item.unread {
    background-color: #ecf5ff;
}

.notification-item.unread:hover {
    background-color: #d9ecff;
}

.item-icon {
    flex-shrink: 0;
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background-color: #f5f7fa;
}

.item-content {
    flex: 1;
    min-width: 0;
    cursor: pointer;
}

.item-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
}

.item-title {
    font-size: 15px;
    font-weight: 500;
    color: #303133;
}

.item-time {
    font-size: 13px;
    color: #909399;
}

.item-text {
    font-size: 14px;
    color: #606266;
    margin-bottom: 8px;
    line-height: 1.6;
}

.item-footer {
    font-size: 13px;
    color: #909399;
}

.item-actions {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: flex-end;
}

.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
}
</style>
