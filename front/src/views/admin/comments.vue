<template>
    <div class="comments-container">
        <!-- 状态统计卡片 -->
        <div class="stats-cards">
            <el-card class="stat-card pending">
                <div class="stat-content">
                    <div class="stat-number">{{ pendingCount }}</div>
                    <div class="stat-label">待审核</div>
                </div>
            </el-card>
            <el-card class="stat-card approved">
                <div class="stat-content">
                    <div class="stat-number">{{ approvedCount }}</div>
                    <div class="stat-label">已通过</div>
                </div>
            </el-card>
            <el-card class="stat-card rejected">
                <div class="stat-content">
                    <div class="stat-number">{{ rejectedCount }}</div>
                    <div class="stat-label">已拒绝</div>
                </div>
            </el-card>
        </div>
        
        <el-card shadow="hover" class="comments-card">
            <template #header>
                <div class="card-header">
                    <h2 class="page-title">评论管理</h2>
                    <div class="header-actions">
                        <template v-if="selectedComments.length > 0">
                            <el-dropdown @command="handleBatchAction">
                                <el-button type="primary" size="small">
                                    批量操作 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                                </el-button>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item command="approve">批量通过</el-dropdown-item>
                                        <el-dropdown-item command="reject">批量拒绝</el-dropdown-item>
                                        <el-dropdown-item command="delete">批量删除</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </template>
                        <el-button v-else type="primary" size="small" disabled>
                            批量操作
                        </el-button>
                    </div>
                </div>
            </template>
            
            <!-- 筛选条件 -->
            <div class="filter-section">
                <el-input v-model="searchKeyword" placeholder="搜索评论内容" size="small" class="filter-input">
                    <template #append>
                        <el-button size="small" :loading="loading" @click="searchComments">
                            <el-icon><Search /></el-icon>
                        </el-button>
                    </template>
                </el-input>
                
                <el-select v-model="statusFilter" placeholder="评论状态" size="small" class="filter-select">
                    <el-option label="全部" value="all"></el-option>
                    <el-option label="已审核" value="approved"></el-option>
                    <el-option label="待审核" value="pending"></el-option>
                    <el-option label="已拒绝" value="rejected"></el-option>
                </el-select>
                
                <el-date-picker
                    v-model="dateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    size="small"
                    class="filter-date"
                ></el-date-picker>
            </div>
            
            <!-- 评论列表 -->
            <el-table 
                :data="commentsList" 
                stripe 
                style="width: 100%"
                :row-key="row => row.id"
                @selection-change="handleSelectionChange"
            >
                <el-table-column type="selection" width="55"></el-table-column>
                <el-table-column prop="id" label="ID" width="80"></el-table-column>
                <el-table-column prop="content" label="评论内容" min-width="300">
                    <template #default="scope">
                        <div class="comment-content">{{ scope.row.content }}</div>
                    </template>
                </el-table-column>
                <el-table-column prop="articleTitle" label="所属文章" min-width="200"></el-table-column>
                <el-table-column prop="userName" label="用户" width="120"></el-table-column>
                <el-table-column prop="createTime" label="评论时间" width="180"></el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="scope">
                        <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180" fixed="right">
                    <template #default="scope">
                        <el-button size="small" circle @click="viewComment(scope.row)">
                            <el-icon><View /></el-icon>
                        </el-button>
                        <el-button v-if="scope.row.status === 'pending'" size="small" type="primary" circle @click="approveComment(scope.row)">
                            <el-icon><Check /></el-icon>
                        </el-button>
                        <el-button v-if="scope.row.status === 'pending'" size="small" type="danger" circle @click="rejectComment(scope.row)">
                            <el-icon><Close /></el-icon>
                        </el-button>
                        <el-button size="small" type="danger" circle @click="deleteComment(scope.row.id)">
                            <el-icon><Delete /></el-icon>
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
            
            <!-- 分页 -->
            <div class="pagination-section">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="total"
                    :pager-count="5"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                ></el-pagination>
            </div>
        </el-card>
        
        <!-- 评论详情对话框 -->
        <el-dialog 
            v-model="dialogVisible" 
            title="评论详情" 
            width="80%"
            :before-close="handleDialogClose"
        >
            <div v-if="currentComment" class="comment-detail">
                <div class="detail-section">
                    <h3 class="section-title">评论信息</h3>
                    <div class="info-row">
                        <span class="info-label">评论内容：</span>
                        <div class="info-value content">{{ currentComment.content }}</div>
                    </div>
                    <div class="info-row">
                        <span class="info-label">所属文章：</span>
                        <div class="info-value">{{ currentComment.articleTitle }}</div>
                    </div>
                    <div class="info-row">
                        <span class="info-label">用户：</span>
                        <div class="info-value">{{ currentComment.userName }}</div>
                    </div>
                    <div class="info-row">
                        <span class="info-label">评论时间：</span>
                        <div class="info-value">{{ currentComment.createTime }}</div>
                    </div>
                    <div class="info-row">
                        <span class="info-label">评论状态：</span>
                        <el-tag :type="getStatusType(currentComment.status)">{{ getStatusText(currentComment.status) }}</el-tag>
                    </div>
                </div>
                <div class="detail-section reply-section">
                    <h3 class="section-title">回复</h3>
                    <el-input
                        v-model="replyContent"
                        type="textarea"
                        placeholder="输入回复内容"
                        rows="4"
                        class="reply-input"
                    ></el-input>
                    <div class="reply-actions">
                        <el-button size="small" @click="dialogVisible = false">取消</el-button>
                        <el-button size="small" type="primary" :loading="replying" @click="replyComment">回复</el-button>
                    </div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, View, Check, Close, Delete, ArrowDown } from '@element-plus/icons-vue'

import logger from '@/utils/logger'
// 搜索和筛选条件
const searchKeyword = ref('')
const statusFilter = ref('all')
const dateRange = ref([])

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)

// 加载状态
const loading = ref(false)
const replying = ref(false)

// 选中的评论
const selectedComments = ref([])

// 评论列表数据
const commentsList = ref([
    {
        id: 1,
        content: '文章写得很棒，学习了很多知识！',
        articleTitle: 'Vue 3 Composition API 实践',
        userName: '用户1',
        createTime: '2024-01-01 10:00:00',
        status: 'approved'
    },
    {
        id: 2,
        content: '有个问题想请教一下，关于setup函数的使用',
        articleTitle: 'Vue 3 Composition API 实践',
        userName: '用户2',
        createTime: '2024-01-01 11:30:00',
        status: 'pending'
    },
    {
        id: 3,
        content: '期待更多优质内容！',
        articleTitle: 'TypeScript 进阶技巧',
        userName: '用户3',
        createTime: '2024-01-02 09:15:00',
        status: 'approved'
    },
    {
        id: 4,
        content: '非常实用的教程，谢谢分享',
        articleTitle: 'Tailwind CSS 最佳实践',
        userName: '用户4',
        createTime: '2024-01-02 14:20:00',
        status: 'approved'
    },
    {
        id: 5,
        content: '前端性能优化确实很重要',
        articleTitle: '前端性能优化策略',
        userName: '用户5',
        createTime: '2024-01-03 16:45:00',
        status: 'pending'
    }
])

// 对话框相关
const dialogVisible = ref(false)
const currentComment = ref(null)
const replyContent = ref('')

// 状态统计计算属性
const pendingCount = computed(() => {
    return commentsList.value.filter(item => item.status === 'pending').length
})

const approvedCount = computed(() => {
    return commentsList.value.filter(item => item.status === 'approved').length
})

const rejectedCount = computed(() => {
    return commentsList.value.filter(item => item.status === 'rejected').length
})

// 获取状态类型
const getStatusType = (status) => {
    switch (status) {
        case 'approved':
            return 'success'
        case 'pending':
            return 'warning'
        case 'rejected':
            return 'danger'
        default:
            return ''
    }
}

// 获取状态文本
const getStatusText = (status) => {
    switch (status) {
        case 'approved':
            return '已审核'
        case 'pending':
            return '待审核'
        case 'rejected':
            return '已拒绝'
        default:
            return status
    }
}

// 处理选择变化
const handleSelectionChange = (selection) => {
    selectedComments.value = selection
}

// 批量操作
const handleBatchAction = (action) => {
    const selectedIds = selectedComments.value.map(item => item.id)
    switch (action) {
        case 'approve':
            selectedComments.value.forEach(comment => {
                comment.status = 'approved'
            })
            logger.debug('批量通过评论:', selectedIds)
            break
        case 'reject':
            selectedComments.value.forEach(comment => {
                comment.status = 'rejected'
            })
            logger.debug('批量拒绝评论:', selectedIds)
            break
        case 'delete':
            commentsList.value = commentsList.value.filter(item => !selectedIds.includes(item.id))
            selectedComments.value = []
            logger.debug('批量删除评论:', selectedIds)
            break
    }
}

// 搜索评论
const searchComments = () => {
    loading.value = true
    // 模拟API调用
    setTimeout(() => {
        logger.debug('搜索评论:', searchKeyword.value)
        loading.value = false
    }, 500)
}

// 查看评论
const viewComment = (comment) => {
    currentComment.value = comment
    replyContent.value = ''
    dialogVisible.value = true
}

// 审核通过评论
const approveComment = (comment) => {
    comment.status = 'approved'
    // 调用API更新评论状态
    logger.debug('审核通过评论:', comment.id)
}

// 拒绝评论
const rejectComment = (comment) => {
    comment.status = 'rejected'
    // 调用API更新评论状态
    logger.debug('拒绝评论:', comment.id)
}

// 删除评论
const deleteComment = (id) => {
    // 调用API删除评论
    logger.debug('删除评论:', id)
    // 从列表中移除
    commentsList.value = commentsList.value.filter(item => item.id !== id)
}

// 回复评论
const replyComment = () => {
    if (!replyContent.value.trim()) {return}
    
    replying.value = true
    // 模拟API调用
    setTimeout(() => {
        logger.debug('回复评论:', currentComment.value.id, replyContent.value)
        dialogVisible.value = false
        replying.value = false
    }, 500)
}

// 处理对话框关闭
const handleDialogClose = () => {
    dialogVisible.value = false
    setTimeout(() => {
        currentComment.value = null
        replyContent.value = ''
    }, 300)
}

// 分页处理
const handleSizeChange = (size) => {
    pageSize.value = size
    // 重新获取数据
    logger.debug('每页条数改变:', size)
}

const handleCurrentChange = (current) => {
    currentPage.value = current
    // 重新获取数据
    logger.debug('当前页码改变:', current)
}
</script>

<style scoped>
.comments-container {
    padding: 20px;
    max-width: 1400px;
    margin: 0 auto;
}

/* 状态统计卡片 */
.stats-cards {
    display: flex;
    gap: 20px;
    margin-bottom: 30px;
    flex-wrap: wrap;
}

.stat-card {
    flex: 1;
    min-width: 200px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
}

.stat-card.pending {
    border-top: 4px solid #E6A23C;
}

.stat-card.approved {
    border-top: 4px solid #67C23A;
}

.stat-card.rejected {
    border-top: 4px solid #F56C6C;
}

.stat-content {
    text-align: center;
    padding: 20px 0;
}

.stat-number {
    font-size: 28px;
    font-weight: bold;
    margin-bottom: 8px;
}

.stat-card.pending .stat-number {
    color: #E6A23C;
}

.stat-card.approved .stat-number {
    color: #67C23A;
}

.stat-card.rejected .stat-number {
    color: #F56C6C;
}

.stat-label {
    font-size: 14px;
    color: #606266;
}

/* 评论卡片 */
.comments-card {
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    height: 60px;
}

.page-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin: 0;
}

.header-actions {
    display: flex;
    gap: 10px;
}

/* 筛选条件 */
.filter-section {
    display: flex;
    gap: 16px;
    margin-bottom: 20px;
    flex-wrap: wrap;
    align-items: center;
}

.filter-input {
    width: 300px;
}

.filter-select {
    width: 150px;
}

.filter-date {
    width: 280px;
}

/* 评论列表 */
.comment-content {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.5;
}

/* 分页 */
.pagination-section {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
}

/* 评论详情对话框 */
.comment-detail {
    padding: 20px;
}

.detail-section {
    margin-bottom: 30px;
}

.section-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid #EBEEF5;
}

.info-row {
    display: flex;
    margin-bottom: 12px;
    align-items: flex-start;
}

.info-label {
    width: 100px;
    font-weight: 500;
    color: #606266;
    flex-shrink: 0;
}

.info-value {
    flex: 1;
    color: #303133;
    line-height: 1.5;
}

.info-value.content {
    padding: 12px;
    background-color: #F5F7FA;
    border-radius: 4px;
    min-height: 80px;
}

.reply-section {
    margin-top: 30px;
}

.reply-input {
    width: 100%;
    margin-bottom: 16px;
}

.reply-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .comments-container {
        padding: 10px;
    }
    
    .stats-cards {
        flex-direction: column;
    }
    
    .stat-card {
        min-width: 100%;
    }
    
    .filter-section {
        flex-direction: column;
        align-items: stretch;
    }
    
    .filter-input,
    .filter-select,
    .filter-date {
        width: 100%;
    }
    
    .card-header {
        flex-direction: column;
        align-items: flex-start;
        height: auto;
        padding: 16px;
        gap: 10px;
    }
    
    .header-actions {
        width: 100%;
        justify-content: flex-end;
    }
    
    .info-row {
        flex-direction: column;
    }
    
    .info-label {
        width: 100%;
        margin-bottom: 4px;
    }
}
</style>
