<template>
    <el-popover
        :visible="visible"
        placement="bottom"
        :width="400"
        trigger="click"
        @show="handleShow"
        @hide="handleHide"
    >
        <template #reference>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
                <el-button :icon="Bell" circle @click="togglePopover" />
            </el-badge>
        </template>

        <div class="notification-center">
            <!-- 头部 -->
            <div class="notification-header">
                <span class="title">通知中心</span>
                <div class="actions">
                    <el-button 
                        link 
                        size="small" 
                        :disabled="unreadCount === 0"
                        @click="handleMarkAllRead"
                    >
                        全部已读
                    </el-button>
                    <el-button 
                        link 
                        size="small" 
                        @click="handleClearAll"
                    >
                        清空
                    </el-button>
                </div>
            </div>

            <!-- 通知列表 -->
            <div class="notification-list">
                <el-scrollbar max-height="400px">
                    <div v-if="loading" class="loading">
                        <el-icon class="is-loading"><Loading /></el-icon>
                        <span>加载中...</span>
                    </div>

                    <div v-else-if="notifications.length === 0" class="empty">
                        <el-empty description="暂无通知" :image-size="80" />
                    </div>

                    <div v-else>
                        <div
                            v-for="item in notifications"
                            :key="item.id"
                            class="notification-item"
                            :class="{ 'unread': !item.is_read }"
                            @click="handleItemClick(item)"
                        >
                            <div class="item-icon">
                                <el-icon v-if="item.type === 'COMMENT'" color="#409EFF">
                                    <ChatDotRound />
                                </el-icon>
                                <el-icon v-else-if="item.type === 'LIKE'" color="#F56C6C">
                                    <Star />
                                </el-icon>
                                <el-icon v-else color="#E6A23C">
                                    <Bell />
                                </el-icon>
                            </div>

                            <div class="item-content">
                                <div class="item-title">{{ item.title }}</div>
                                <div v-if="item.content" class="item-text">{{ item.content }}</div>
                                <div class="item-time">{{ formatTime(item.created_time) }}</div>
                            </div>

                            <div class="item-actions">
                                <el-button
                                    link
                                    size="small"
                                    @click.stop="handleDelete(item.id)"
                                >
                                    <el-icon><Close /></el-icon>
                                </el-button>
                            </div>
                        </div>
                    </div>
                </el-scrollbar>
            </div>

            <!-- 底部 -->
            <div class="notification-footer">
                <el-button link @click="goToNotificationPage">
                    查看全部通知
                </el-button>
            </div>
        </div>
    </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    Bell, ChatDotRound, Star, Close, Loading
} from '@element-plus/icons-vue'
import {
    getNotificationList,
    getUnreadCount,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    clearAllNotifications
} from '@/api/notification'
import { fromNow } from '@/utils/dayjs'

import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'
const router = useRouter()

// 数据
const visible = ref(false)
const loading = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
let pollingTimer = null

// 切换弹窗
const togglePopover = () => {
    visible.value = !visible.value
}

// 显示时加载数据
const handleShow = () => {
    loadNotifications()
}

const handleHide = () => {
    // 可以在这里做一些清理工作
}

// 加载通知列表
const loadNotifications = async () => {
    try {
        loading.value = true
        const res = await getNotificationList(1, 10)
        if (res.code === API_STATUS.SUCCESS) {
            notifications.value = res.data.list
        }
    } catch (error) {
        logger.error('加载通知失败:', error)
    } finally {
        loading.value = false
    }
}

// 加载未读数量
const loadUnreadCount = async () => {
    try {
        const res = await getUnreadCount()
        if (res.code === API_STATUS.SUCCESS) {
            unreadCount.value = res.data
        }
    } catch (error) {
        logger.error('加载未读数量失败:', error)
    }
}

// 点击通知项
const handleItemClick = async (item) => {
    // 标记为已读
    if (!item.is_read) {
        await markAsRead(item.id)
        item.is_read = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
    }

    // 跳转到目标页面
    if (item.target_type === 'ARTICLE' && item.target_id) {
        router.push({
            path: '/article/detail',
            query: { articleId: item.target_id }
        })
        visible.value = false
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
        const res = await deleteNotification(id)
        if (res.code === API_STATUS.SUCCESS) {
            const index = notifications.value.findIndex(item => item.id === id)
            if (index > -1) {
                const item = notifications.value[index]
                if (!item.is_read) {
                    unreadCount.value = Math.max(0, unreadCount.value - 1)
                }
                notifications.value.splice(index, 1)
            }
            ElMessage.success('删除成功')
        }
    } catch (error) {
        ElMessage.error('删除失败')
    }
}

// 跳转到通知页面
const goToNotificationPage = () => {
    router.push('/user/notifications')
    visible.value = false
}

// 格式化时间
const formatTime = (time) => {
    return fromNow(time)
}

// 开始轮询
const startPolling = () => {
    // 每30秒检查一次未读数量
    pollingTimer = setInterval(() => {
        loadUnreadCount()
    }, 30000)
}

// 停止轮询
const stopPolling = () => {
    if (pollingTimer) {
        clearInterval(pollingTimer)
        pollingTimer = null
    }
}

// 组件挂载
onMounted(() => {
    loadUnreadCount()
    startPolling()
})

// 组件卸载
onUnmounted(() => {
    stopPolling()
})
</script>

<style scoped>
.notification-center {
    display: flex;
    flex-direction: column;
}

.notification-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
}

.notification-header .title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.notification-header .actions {
    display: flex;
    gap: 8px;
}

.notification-list {
    min-height: 200px;
    max-height: 400px;
}

.loading,
.empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    color: #909399;
}

.loading {
    gap: 8px;
}

.notification-item {
    display: flex;
    gap: 12px;
    padding: 12px 16px;
    cursor: pointer;
    transition: background-color 0.2s;
    border-bottom: 1px solid #f0f0f0;
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
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background-color: #f5f7fa;
}

.item-content {
    flex: 1;
    min-width: 0;
}

.item-title {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
}

.item-text {
    font-size: 13px;
    color: #606266;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.item-time {
    font-size: 12px;
    color: #909399;
}

.item-actions {
    flex-shrink: 0;
    display: flex;
    align-items: flex-start;
}

.notification-footer {
    padding: 12px 16px;
    border-top: 1px solid #f0f0f0;
    text-align: center;
}
</style>
