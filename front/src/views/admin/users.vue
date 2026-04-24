<template>
    <div class="users-container">
        <el-card shadow="hover" class="users-card">
            <template #header>
                <div class="card-header">
                    <h2 class="page-title">用户管理</h2>
                    <div class="header-actions">
                        <el-button type="primary" :icon="Plus" size="default" @click="addUser">
                            添加用户
                        </el-button>
                    </div>
                </div>
            </template>
            
            <!-- 统计信息卡片 -->
            <div class="stats-cards">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-value">{{ usersList.length }}</div>
                        <div class="stat-label">总用户数</div>
                    </div>
                </el-card>
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-value">{{ enabledUsersCount }}</div>
                        <div class="stat-label">启用用户</div>
                    </div>
                </el-card>
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-value">{{ disabledUsersCount }}</div>
                        <div class="stat-label">禁用用户</div>
                    </div>
                </el-card>
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-value">{{ adminUsersCount }}</div>
                        <div class="stat-label">管理员</div>
                    </div>
                </el-card>
            </div>
            
            <!-- 筛选条件 -->
            <div class="filter-section">
                <el-input 
                    v-model="searchKeyword" 
                    placeholder="搜索用户名或邮箱" 
                    :prefix-icon="Search"
                    size="default"
                    class="search-input"
                    @keyup.enter="searchUsers"
                >
                    <template #append>
                        <el-button 
                            type="primary" 
                            :icon="Search"
                            @click="searchUsers"
                        >
                            搜索
                        </el-button>
                    </template>
                </el-input>
                
                <div class="filter-options">
                    <el-select 
                        v-model="roleFilter" 
                        placeholder="用户角色" 
                        size="default"
                        class="filter-select"
                    >
                        <el-option label="全部" value="all"></el-option>
                        <el-option label="管理员" value="admin"></el-option>
                        <el-option label="普通用户" value="user"></el-option>
                    </el-select>
                    
                    <el-select 
                        v-model="statusFilter" 
                        placeholder="用户状态" 
                        size="default"
                        class="filter-select"
                    >
                        <el-option label="全部" value="all"></el-option>
                        <el-option label="启用" value="enabled"></el-option>
                        <el-option label="禁用" value="disabled"></el-option>
                    </el-select>
                </div>
            </div>
            
            <!-- 用户列表 -->
            <el-table 
                :data="filteredUsersList" 
                stripe 
                style="width: 100%"
                :loading="loading"
                element-loading-text="加载中..."
                element-loading-spinner="el-icon-loading"
                element-loading-background="rgba(255, 255, 255, 0.8)"
                class="users-table"
                @selection-change="handleSelectionChange"
            >
                <el-table-column type="selection" width="60" align="center"></el-table-column>
                <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
                <el-table-column prop="userName" label="用户名" min-width="180">
                    <template #default="scope">
                        <div class="user-info">
                            <div class="user-avatar">
                                {{ scope.row.userName.charAt(0).toUpperCase() }}
                            </div>
                            <span class="user-name">{{ scope.row.userName }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="email" label="邮箱" min-width="220"></el-table-column>
                <el-table-column prop="role" label="角色" width="120" align="center">
                    <template #default="scope">
                        <el-tag 
                            :type="scope.row.role === 'admin' ? 'danger' : 'success'"
                            effect="dark"
                            class="role-tag"
                        >
                            {{ scope.row.role === 'admin' ? '管理员' : '普通用户' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="120" align="center">
                    <template #default="scope">
                        <el-tag 
                            :type="scope.row.status === 'enabled' ? 'success' : 'danger'"
                            effect="dark"
                            class="status-tag"
                        >
                            {{ scope.row.status === 'enabled' ? '启用' : '禁用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="180" align="center">
                    <template #default="scope">
                        {{ formatDate(scope.row.createTime) }}
                    </template>
                </el-table-column>
                <el-table-column prop="lastLoginTime" label="最后登录" width="180" align="center">
                    <template #default="scope">
                        {{ formatDate(scope.row.lastLoginTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="220" fixed="right" align="center">
                    <template #default="scope">
                        <el-button 
                            size="small" 
                            :icon="View"
                            class="action-btn view-btn"
                            @click="viewUser(scope.row)"
                        >
                            查看
                        </el-button>
                        <el-button 
                            size="small" 
                            type="primary" 
                            :icon="Edit"
                            class="action-btn edit-btn"
                            @click="editUser(scope.row)"
                        >
                            编辑
                        </el-button>
                        <el-button 
                            size="small" 
                            :type="scope.row.status === 'enabled' ? 'danger' : 'success'"
                            :icon="scope.row.status === 'enabled' ? Delete : Check"
                            class="action-btn"
                            @click="toggleUserStatus(scope.row)"
                        >
                            {{ scope.row.status === 'enabled' ? '禁用' : '启用' }}
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
            
            <!-- 分页 -->
            <div class="pagination-section">
                <div class="pagination-info">
                    共 {{ total }} 条记录
                </div>
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="total"
                    class="pagination"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                ></el-pagination>
            </div>
        </el-card>
        
        <!-- 用户详情对话框 -->
        <el-dialog 
            v-model="dialogVisible" 
            :title="dialogTitle" 
            width="600px"
            :close-on-click-modal="false"
            :show-close="true"
        >
            <div v-if="currentUser">
                <el-form :model="currentUser" label-width="100px" class="user-form">
                    <el-form-item label="用户名">
                        <el-input 
                            v-model="currentUser.userName" 
                            :disabled="dialogTitle === '用户详情'"
                            placeholder="请输入用户名"
                        ></el-input>
                    </el-form-item>
                    <el-form-item label="邮箱">
                        <el-input 
                            v-model="currentUser.email" 
                            :disabled="dialogTitle === '用户详情'"
                            placeholder="请输入邮箱"
                        ></el-input>
                    </el-form-item>
                    <el-form-item label="角色">
                        <el-select 
                            v-model="currentUser.role" 
                            :disabled="dialogTitle === '用户详情'"
                            placeholder="请选择角色"
                        >
                            <el-option label="管理员" value="admin"></el-option>
                            <el-option label="普通用户" value="user"></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="状态">
                        <el-select 
                            v-model="currentUser.status" 
                            :disabled="dialogTitle === '用户详情'"
                            placeholder="请选择状态"
                        >
                            <el-option label="启用" value="enabled"></el-option>
                            <el-option label="禁用" value="disabled"></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="创建时间">
                        <el-input v-model="currentUser.createTime" disabled></el-input>
                    </el-form-item>
                    <el-form-item label="最后登录">
                        <el-input v-model="currentUser.lastLoginTime" disabled></el-input>
                    </el-form-item>
                </el-form>
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="dialogVisible = false">关闭</el-button>
                    <el-button 
                        v-if="dialogTitle !== '用户详情'" 
                        type="primary" 
                        @click="saveUser"
                    >
                        保存
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Plus, View, Edit, Delete, Check } from '@element-plus/icons-vue'

import logger from '@/utils/logger'
// 搜索和筛选条件
const searchKeyword = ref('')
const roleFilter = ref('all')
const statusFilter = ref('all')

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)
const loading = ref(false)

// 用户列表数据
const usersList = ref([
    {
        id: 1,
        userName: 'admin',
        email: 'admin@example.com',
        role: 'admin',
        status: 'enabled',
        createTime: '2024-01-01 00:00:00',
        lastLoginTime: '2024-01-05 10:00:00'
    },
    {
        id: 2,
        userName: 'user1',
        email: 'user1@example.com',
        role: 'user',
        status: 'enabled',
        createTime: '2024-01-02 00:00:00',
        lastLoginTime: '2024-01-04 15:30:00'
    },
    {
        id: 3,
        userName: 'user2',
        email: 'user2@example.com',
        role: 'user',
        status: 'disabled',
        createTime: '2024-01-03 00:00:00',
        lastLoginTime: '2024-01-03 09:00:00'
    },
    {
        id: 4,
        userName: 'user3',
        email: 'user3@example.com',
        role: 'user',
        status: 'enabled',
        createTime: '2024-01-04 00:00:00',
        lastLoginTime: '2024-01-05 14:00:00'
    },
    {
        id: 5,
        userName: 'user4',
        email: 'user4@example.com',
        role: 'user',
        status: 'enabled',
        createTime: '2024-01-05 00:00:00',
        lastLoginTime: '2024-01-05 16:00:00'
    }
])

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('用户详情')
const currentUser = ref(null)
const multipleSelection = ref([])

// 计算属性
const enabledUsersCount = computed(() => {
    return usersList.value.filter(user => user.status === 'enabled').length
})

const disabledUsersCount = computed(() => {
    return usersList.value.filter(user => user.status === 'disabled').length
})

const adminUsersCount = computed(() => {
    return usersList.value.filter(user => user.role === 'admin').length
})

const filteredUsersList = computed(() => {
    let filtered = [...usersList.value]
    
    // 搜索筛选
    if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        filtered = filtered.filter(user => 
            user.userName.toLowerCase().includes(keyword) ||
            user.email.toLowerCase().includes(keyword)
        )
    }
    
    // 角色筛选
    if (roleFilter.value !== 'all') {
        filtered = filtered.filter(user => user.role === roleFilter.value)
    }
    
    // 状态筛选
    if (statusFilter.value !== 'all') {
        filtered = filtered.filter(user => user.status === statusFilter.value)
    }
    
    return filtered
})

// 方法
const formatDate = (dateString) => {
    if (!dateString) {return '-'}
    return dateString
}

const searchUsers = () => {
    loading.value = true
    // 模拟API请求延迟
    setTimeout(() => {
        logger.debug('搜索用户:', searchKeyword.value)
        loading.value = false
    }, 500)
}

const addUser = () => {
    currentUser.value = {
        userName: '',
        email: '',
        role: 'user',
        status: 'enabled',
        createTime: new Date().toLocaleString(),
        lastLoginTime: ''
    }
    dialogTitle.value = '添加用户'
    dialogVisible.value = true
}

const viewUser = (user) => {
    currentUser.value = { ...user }
    dialogTitle.value = '用户详情'
    dialogVisible.value = true
}

const editUser = (user) => {
    currentUser.value = { ...user }
    dialogTitle.value = '编辑用户'
    dialogVisible.value = true
}

const saveUser = () => {
    // 模拟保存操作
    loading.value = true
    setTimeout(() => {
        logger.debug('保存用户:', currentUser.value)
        dialogVisible.value = false
        loading.value = false
    }, 500)
}

const toggleUserStatus = (user) => {
    loading.value = true
    setTimeout(() => {
        user.status = user.status === 'enabled' ? 'disabled' : 'enabled'
        logger.debug('切换用户状态:', user.id, user.status)
        loading.value = false
    }, 300)
}

const handleSelectionChange = (val) => {
    multipleSelection.value = val
}

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

// 生命周期
onMounted(() => {
    // 初始化数据
    logger.debug('用户管理页面加载完成')
})
</script>

<style scoped>
.users-container {
    padding: 20px;
    min-height: 100vh;
    background-color: var(--bg-secondary);
}

.users-card {
    border-radius: 8px;
    overflow: hidden;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    background-color: var(--bg-card);
    border-bottom: 1px solid var(--border-color);
}

.page-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
}

.header-actions {
    display: flex;
    gap: 10px;
}

.stats-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
    margin-bottom: 24px;
}

.stat-card {
    border-radius: 8px;
    transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.stat-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
    text-align: center;
    padding: 20px;
}

.stat-value {
    font-size: 24px;
    font-weight: 600;
    color: var(--color-accent);
    margin-bottom: 8px;
}

.stat-label {
    font-size: 14px;
    color: var(--text-secondary);
}

.filter-section {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 24px;
    padding: 20px;
    background-color: var(--bg-secondary);
    border-radius: 8px;
}

.search-input {
    flex: 1;
    min-width: 300px;
}

.filter-options {
    display: flex;
    gap: 16px;
}

.filter-select {
    width: 160px;
}

.users-table {
    border-radius: 8px;
    overflow: hidden;
}

.user-info {
    display: flex;
    align-items: center;
    gap: 12px;
}

.user-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background-color: var(--color-accent);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 14px;
}

.user-name {
    font-weight: 500;
    color: var(--text-primary);
}

.role-tag, .status-tag {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 10px;
}

.action-btn {
    margin-right: 8px;
    transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.action-btn:hover {
    transform: translateY(-1px);
}

.view-btn {
    color: var(--color-accent);
}

.edit-btn {
    color: var(--color-success);
}

.pagination-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 24px;
    padding: 0 20px;
}

.pagination-info {
    font-size: 14px;
    color: var(--text-secondary);
}

.pagination {
    margin: 0;
}

.user-form {
    padding: 20px 0;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding: 16px 24px;
    border-top: 1px solid var(--border-color);
}

/* 响应式设计 */
@media (max-width: 768px) {
    .users-container {
        padding: 10px;
    }
    
    .card-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
    }
    
    .stats-cards {
        grid-template-columns: 1fr;
    }
    
    .filter-section {
        flex-direction: column;
    }
    
    .search-input {
        width: 100%;
        min-width: unset;
    }
    
    .filter-options {
        width: 100%;
        justify-content: space-between;
    }
    
    .filter-select {
        flex: 1;
    }
    
    .pagination-section {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
    }
}
</style>
