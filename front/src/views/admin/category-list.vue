<template>
    <div class="category-management-container">
        <!-- 搜索栏 -->
        <el-card shadow="hover" :body-style="{ padding: '20px' }" class="mb-5 border-1 search-card">
            <div class="search-form">
                <div class="form-item">
                    <el-text class="form-label">分类名称</el-text>
                    <el-input 
                        v-model="searchCategoryName" 
                        placeholder="请输入（模糊查询）" 
                        class="search-input" 
                        clearable
                        @keyup.enter="getTableData"
                    />
                </div>
                <div class="form-item">
                    <el-text class="form-label">创建日期</el-text>
                    <el-date-picker 
                        v-model="pickDate" 
                        type="daterange" 
                        range-separator="至" 
                        start-placeholder="开始时间"
                        end-placeholder="结束时间" 
                        :shortcuts="shortcuts" 
                        size="default" 
                        @change="datepickerChange" 
                    />
                </div>
                <div class="form-actions">
                    <el-button 
                        type="primary" 
                        :icon="Search" 
                        class="action-button"
                        @click="getTableData"
                    >
                        查询
                    </el-button>
                    <el-button 
                        :icon="RefreshRight" 
                        class="action-button"
                        @click="reset"
                    >
                        重置
                    </el-button>
                </div>
            </div>
        </el-card>

        <!-- 分类列表 -->
        <el-card shadow="hover" class="border-1 list-card">
            <!-- 新增按钮 -->
            <div class="card-header">
                <h3 class="card-title">分类管理</h3>
                <el-button 
                    type="primary" 
                    class="add-button"
                    @click="openAddDialog"
                >
                    <el-icon class="mr-1">
                        <Plus />
                    </el-icon>
                    新增分类
                </el-button>
            </div>

            <!-- 表格 -->
            <el-table 
                v-loading="tableLoading" 
                :data="tableData" 
                stripe 
                style="width: 100%"
                class="category-table"
                :row-class-name="tableRowClassName"
            >
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="name" label="分类名称" min-width="180">
                    <template #default="scope">
                        <el-tag size="medium" effect="light">{{ scope.row.name }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="200" />
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="scope">
                        <div class="table-actions">
                            <el-button 
                                type="primary" 
                                size="small" 
                                class="action-btn edit-btn"
                                @click="openEditDialog(scope.row)"
                            >
                                <el-icon class="mr-1">
                                    <Edit />
                                </el-icon>
                                编辑
                            </el-button>
                            <el-button 
                                type="danger" 
                                size="small" 
                                class="action-btn delete-btn"
                                @click="deleteCategorySubmit(scope.row)"
                            >
                                <el-icon class="mr-1">
                                    <Delete />
                                </el-icon>
                                删除
                            </el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination-container">
                <el-pagination 
                    v-model:current-page="current" 
                    v-model:page-size="size" 
                    :page-sizes="[10, 20, 50]"
                    background="true" 
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="total" 
                    @size-change="handleSizeChange" 
                    @current-change="getTableData" 
                />
            </div>
        </el-card>

        <!-- 新增分类对话框 -->
        <el-dialog 
            v-model="isAddCatagoryDialogShow" 
            title="新增分类" 
            width="400px" 
            :show-close="true" 
            draggable
            destroy-on-close
        >
            <el-form 
                ref="addFormRef" 
                :model="addForm" 
                label-position="top" 
                :size="'large'" 
                :rules="rules"
                class="category-form"
            >
                <el-form-item label="分类名称" prop="name">
                    <el-input 
                        v-model="addForm.name" 
                        autocomplete="off" 
                        size="large" 
                        maxlength="10" 
                        show-word-limit 
                        clearable 
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="isAddCatagoryDialogShow = false">取消</el-button>
                    <el-button type="primary" @click="addCategorySubmit">
                        提交
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 编辑分类对话框 -->
        <el-dialog 
            v-model="isEditCatagoryDialogShow" 
            title="编辑分类" 
            width="400px" 
            :show-close="true" 
            draggable
            destroy-on-close
        >
            <el-form 
                ref="editFormRef" 
                :model="editForm" 
                label-position="top" 
                :size="'large'" 
                :rules="rules"
                class="category-form"
            >
                <el-form-item label="分类名称" prop="name">
                    <el-input 
                        v-model="editForm.name" 
                        autocomplete="off" 
                        size="large" 
                        maxlength="10" 
                        show-word-limit 
                        clearable 
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="isEditCatagoryDialogShow = false">取消</el-button>
                    <el-button type="primary" @click="editCategorySubmit">
                        保存
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { addCategory, getCategoryPageList, deleteCategory, updateCategory } from '@/api/admin/category'
import { showMessage } from '@/utils'
import { ElMessageBox } from 'element-plus'
import dayjs from '@/utils/dayjs'
import { Search, RefreshRight, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { API_STATUS } from '@/composables/api'

// 对话框状态
const isAddCatagoryDialogShow = ref(false)
const isEditCatagoryDialogShow = ref(false)

// 搜索条件
const searchCategoryName = ref('')
const pickDate = ref('')
const startDate = reactive({})
const endDate = reactive({})

// 表单数据
const addForm = reactive({ name: '' })
const editForm = reactive({ id: '', name: '' })
const addFormRef = ref(null)
const editFormRef = ref(null)

// 表单验证规则
const rules = {
    name: [
        { required: true, message: '请输入分类名称', trigger: 'blur' },
        { min: 1, max: 10, message: '分类名称字数要求1-10个字符', trigger: 'blur' }
    ]
}

// 日期选择器快捷选项
const shortcuts = [
    {
        text: '最近一周',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
            return [start, end]
        }
    },
    {
        text: '最近一个月',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
            return [start, end]
        }
    },
    {
        text: '最近三个月',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
            return [start, end]
        }
    }
]

// 表格数据
const tableLoading = ref(false)
const tableData = ref([])
const current = ref(1)
const total = ref(0)
const size = ref(10)

// 重置搜索条件
const reset = () => {
    pickDate.value = ''
    startDate.value = null
    endDate.value = null
    searchCategoryName.value = ''
}

// 日期选择变化
const datepickerChange = (e) => {
    if (e) {
        startDate.value = dayjs(e[0]).format('YYYY-MM-DD HH:mm:ss')
        endDate.value = dayjs(e[1]).format('YYYY-MM-DD HH:mm:ss')
    } else {
        startDate.value = null
        endDate.value = null
    }
}

// 打开新增对话框
const openAddDialog = () => {
    addForm.name = ''
    if (addFormRef.value) {
        addFormRef.value.resetFields()
    }
    isAddCatagoryDialogShow.value = true
}

// 打开编辑对话框
const openEditDialog = (row) => {
    editForm.id = row.id
    editForm.name = row.name
    if (editFormRef.value) {
        editFormRef.value.resetFields()
    }
    isEditCatagoryDialogShow.value = true
}

// 新增分类提交
const addCategorySubmit = () => {
    if (addFormRef.value) {
        addFormRef.value.validate((valid) => {
            if (valid) {
                addCategory(addForm).then((response) => {
                    if (response.code === 200) {
                        showMessage('添加成功', 'success', 'message')
                        isAddCatagoryDialogShow.value = false
                        getTableData()
                    } else {
                        showMessage(response.message, 'warning', 'message')
                    }
                }).catch(() => {
                    showMessage('添加失败，请重试', 'error', 'message')
                })
            }
        })
    }
}

// 编辑分类提交
const editCategorySubmit = () => {
    if (editFormRef.value) {
        editFormRef.value.validate((valid) => {
            if (valid) {
                updateCategory(editForm).then((response) => {
                    if (response.code === 200) {
                        showMessage('编辑成功', 'success', 'message')
                        isEditCatagoryDialogShow.value = false
                        getTableData()
                    } else {
                        showMessage(response.message, 'warning', 'message')
                    }
                }).catch(() => {
                    showMessage('编辑失败，请重试', 'error', 'message')
                })
            }
        })
    }
}

// 获取分页数据
function getTableData() {
    tableLoading.value = true
    getCategoryPageList({
        current: current.value, 
        size: size.value, 
        startDate: startDate.value, 
        endDate: endDate.value, 
        categoryName: searchCategoryName.value 
    })
        .then((res) => {
            if (res.code === API_STATUS.SUCCESS) {
                tableData.value = res.data || []
                current.value = res.current || 1
                total.value = res.total || 0
                size.value = res.size || 10
            }
        })
        .catch(() => {
            showMessage('获取数据失败，请重试', 'error', 'message')
        })
        .finally(() => {
            tableLoading.value = false
        })
}

// 初始加载数据
getTableData()

// 分页大小变化
const handleSizeChange = (e) => {
    size.value = e
    getTableData()
}

// 删除分类
const deleteCategorySubmit = (row) => {
    ElMessageBox.confirm(
        '是否确认要删除该分类?',
        '删除确认',
        {
            confirmButtonText: '确认删除',
            cancelButtonText: '取消',
            type: 'warning',
            center: true
        }
    )
        .then(() => {
            deleteCategory(row.id).then((response) => {
                if (response.code === 200) {
                    showMessage('删除成功', 'success')
                    getTableData()
                } else {
                    showMessage(response.message, 'warning')
                }
            }).catch(() => {
                showMessage('删除失败，请重试', 'error')
            })
        })
        .catch(() => {
            // 取消删除，不做处理
        })
}

// 表格行样式
const tableRowClassName = ({ rowIndex }) => {
    return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}
</script>

<style scoped>
.category-management-container {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
}

/* 搜索栏样式 */
.search-card {
    border-radius: 8px;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.search-card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.search-form {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
}

.form-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.form-label {
    font-weight: 500;
    color: var(--text-primary);
    white-space: nowrap;
}

.search-input {
    min-width: 200px;
    width: 300px;
}

.form-actions {
    display: flex;
    gap: 10px;
    margin-left: auto;
}

.action-button {
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.action-button:hover {
    transform: translateY(-1px);
}

/* 列表卡片样式 */
.list-card {
    border-radius: 8px;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.list-card:hover {
    box-shadow: 0 4px 12px var(--shadow-md);
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid var(--border-color);
}

.card-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
}

.add-button {
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.add-button:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px var(--color-primary-subtle);
}

/* 表格样式 */
.category-table {
    border-radius: 4px;
    overflow: hidden;
}

.category-table th {
    background-color: var(--bg-secondary);
    font-weight: 600;
}

.category-table tr {
    transition: background-color 0.2s ease;
}

.category-table tr:hover {
    background-color: var(--bg-tertiary);
}

.even-row {
    background-color: var(--bg-secondary);
}

.odd-row {
    background-color: var(--bg-card);
}

/* 表格操作按钮 */
.table-actions {
    display: flex;
    gap: 8px;
}

.action-btn {
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
    padding: 4px 12px;
}

.action-btn:hover {
    transform: translateY(-1px);
}

.edit-btn:hover {
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.delete-btn:hover {
    box-shadow: 0 2px 8px rgba(255, 73, 73, 0.3);
}

/* 分页样式 */
.pagination-container {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid var(--border-color);
}

/* 表单样式 */
.category-form {
    margin-top: 20px;
}

/* 对话框样式 */
.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .category-management-container {
        padding: 10px;
    }

    .search-form {
        flex-direction: column;
        align-items: stretch;
    }

    .form-item {
        flex-direction: column;
        align-items: stretch;
        gap: 4px;
    }

    .form-label {
        white-space: normal;
    }

    .search-input {
        width: 100%;
    }

    .form-actions {
        margin-left: 0;
        justify-content: center;
    }

    .card-header {
        flex-direction: column;
        align-items: stretch;
        gap: 10px;
    }

    .table-actions {
        flex-direction: column;
        gap: 4px;
    }

    .action-btn {
        width: 100%;
        justify-content: center;
    }

    .pagination-container {
        justify-content: center;
    }
}

/* 动画效果 */
@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.search-card,
.list-card {
    animation: fadeIn 0.3s ease;
}

/* 加载状态样式 */
:deep(.el-loading-spinner) {
    margin-top: -20px;
}

:deep(.el-loading-text) {
    color: var(--color-accent);
}
</style>
