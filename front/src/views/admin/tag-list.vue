<template>
    <div class="tag-management-container">
        <!-- 搜索和操作区域 -->
        <el-card shadow="hover" :body-style="{ padding: '20px' }" class="mb-5 border-1">
            <div class="search-and-actions">
                <div class="search-section">
                    <el-text class="mr-3 text-gray-600">标签名称</el-text>
                    <el-input 
                        v-model="searchTagName" 
                        placeholder="请输入标签名称进行搜索" 
                        class="w-64 mr-5" 
                        clearable
                        @keyup.enter="getTableData"
                    >
                        <template #prefix>
                            <el-icon class="el-input__icon"><Search /></el-icon>
                        </template>
                    </el-input>

                    <el-text class="mr-3 text-gray-600">创建日期</el-text>
                    <el-date-picker 
                        v-model="pickDate" 
                        type="daterange" 
                        range-separator="至" 
                        start-placeholder="开始时间"
                        end-placeholder="结束时间" 
                        :shortcuts="shortcuts" 
                        size="default" 
                        class="mr-5"
                        @change="datepickerChange"
                    />
                </div>

                <div class="action-buttons">
                    <el-button 
                        type="primary" 
                        :loading="searchLoading"
                        @click="getTableData"
                    >
                        <el-icon class="mr-1"><Search /></el-icon>
                        查询
                    </el-button>
                    <el-button 
                        @click="reset"
                    >
                        <el-icon class="mr-1"><RefreshRight /></el-icon>
                        重置
                    </el-button>
                    <el-button 
                        type="success" 
                        @click="isTagPublishDialogShow = true"
                    >
                        <el-icon class="mr-1"><Plus /></el-icon>
                        新增标签
                    </el-button>
                    <el-button 
                        type="danger" 
                        :disabled="selectedTags.length === 0"
                        @click="batchDeleteTags"
                    >
                        <el-icon class="mr-1"><Delete /></el-icon>
                        批量删除
                    </el-button>
                </div>
            </div>
        </el-card>

        <!-- 标签列表 -->
        <el-card shadow="hover" class="border-1">
            <!-- 统计信息 -->
            <div class="stats-section mb-4">
                <el-statistic 
                    v-for="stat in statistics" 
                    :key="stat.label" 
                    class="mr-8"
                    :title="stat.label"
                    :value="stat.value"
                    :precision="0"
                    :value-style="{ color: stat.color }"
                />
            </div>

            <!-- 表格 -->
            <el-table 
                v-loading="tableLoading" 
                :data="tableData" 
                stripe 
                style="width: 100%" 
                class="mt-4"
                @selection-change="handleSelectionChange"
            >
                <el-table-column type="selection" width="55" />
                <el-table-column prop="name" label="标签名称" min-width="180">
                    <template #default="scope">
                        <el-tag 
                            class="mr-2" 
                            type="info" 
                            effect="light"
                            :closable="false"
                        >
                            {{ scope.row.name }}
                        </el-tag>
                        <el-badge 
                            :value="scope.row.usageCount || 0" 
                            class="item"
                            type="warning"
                            :hidden="!(scope.row.usageCount > 0)"
                        />
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="180" />
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="scope">
                        <el-button 
                            type="primary" 
                            size="small" 
                            class="mr-2"
                            @click="editTag(scope.row)"
                        >
                            <el-icon class="mr-1"><Edit /></el-icon>
                            编辑
                        </el-button>
                        <el-button 
                            type="danger" 
                            size="small" 
                            @click="deleteTagSubmit(scope.row)"
                        >
                            <el-icon class="mr-1"><Delete /></el-icon>
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="mt-5 flex justify-between items-center">
                <div class="text-gray-600">
                    共 {{ total }} 个标签
                </div>
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

        <!-- 新增标签对话框 -->
        <el-dialog 
            v-model="isTagPublishDialogShow" 
            :title="dialogTitle" 
            width="40%" 
            :show-close="false" 
            draggable
            destroy-on-close
        >
            <el-form 
                ref="formRef" 
                :model="form" 
                label-position="top" 
                :size="'default'" 
                :rules="rules"
            >
                <el-form-item label="标签名称" prop="tags">
                    <div class="tags-container">
                        <el-tag 
                            v-for="tag in dynamicTags" 
                            :key="tag" 
                            class="mx-1 mb-2"
                            closable 
                            :disable-transitions="false"
                            type="info" 
                            effect="light" 
                            round
                            @close="handleClose(tag)"
                        >
                            {{ tag }}
                        </el-tag>
                        <el-input 
                            v-if="inputVisible" 
                            ref="InputRef" 
                            v-model="inputValue" 
                            class="ml-1 w-32 mb-2" 
                            size="small"
                            placeholder="输入标签名称" 
                            @keyup.enter="handleInputConfirm"
                            @blur="handleInputConfirm"
                        />
                        <el-button 
                            v-else 
                            class="button-new-tag ml-1 mb-2" 
                            size="small" 
                            round 
                            type="primary"
                            plain
                            @click="showInput"
                        >
                            + 新增标签
                        </el-button>
                    </div>
                    <div class="text-gray-500 text-sm mt-2">
                        提示：按回车键添加标签，点击标签右侧 × 移除标签
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="isTagPublishDialogShow = false">取消</el-button>
                    <el-button 
                        type="primary" 
                        :loading="submitLoading"
                        @click="addTagsSubmit"
                    >
                        提交
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 编辑标签对话框 -->
        <el-dialog 
            v-model="isEditDialogShow" 
            title="编辑标签" 
            width="30%" 
            :show-close="false" 
            draggable
        >
            <el-form 
                ref="editFormRef" 
                :model="editForm" 
                label-position="top" 
                :size="'default'" 
                :rules="editRules"
            >
                <el-form-item label="标签名称" prop="name">
                    <el-input 
                        v-model="editForm.name" 
                        placeholder="请输入标签名称"
                        clearable
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="isEditDialogShow = false">取消</el-button>
                    <el-button 
                        type="primary" 
                        :loading="editSubmitLoading"
                        @click="updateTagSubmit"
                    >
                        保存
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { addTags, getTagPageList, deleteTag, updateTag } from '@/api/admin/tag'
import { showMessage } from '@/utils'
import dayjs from '@/utils/dayjs'
import { Search, RefreshRight, Plus, Delete, Edit } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { API_STATUS } from '@/composables/api'

// 对话框状态
const isTagPublishDialogShow = ref(false)
const isEditDialogShow = ref(false)
const dialogTitle = ref('新增标签')

// 搜索相关
const searchTagName = ref('')
const pickDate = ref('')
const startDate = reactive({})
const endDate = reactive({})
const searchLoading = ref(false)

// 表单相关
const form = reactive({ tags: [] })
const editForm = reactive({ id: '', name: '' })
const formRef = ref(null)
const editFormRef = ref(null)
const submitLoading = ref(false)
const editSubmitLoading = ref(false)

// 标签输入相关
const inputValue = ref('')
const dynamicTags = ref([])
const inputVisible = ref(false)
const InputRef = ref(null)

// 表格相关
const tableLoading = ref(false)
const tableData = ref([])
const current = ref(1)
const total = ref(0)
const size = ref(10)
const selectedTags = ref([])

// 统计信息
const statistics = reactive([
    { label: '总标签数', value: 0, color: '#409EFF' },
    { label: '使用中标签', value: 0, color: '#67C23A' },
    { label: '未使用标签', value: 0, color: '#E6A23C' }
])

// 日期快捷选项
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

// 表单验证规则
const rules = {
    tags: [
        {
            validator: (rule, value, callback) => {
                if (dynamicTags.value.length === 0) {
                    callback(new Error('请至少添加一个标签'))
                } else {
                    callback()
                }
            },
            trigger: 'blur'
        }
    ]
}

const editRules = {
    name: [
        {
            required: true,
            message: '请输入标签名称',
            trigger: 'blur'
        },
        {
            min: 1,
            max: 20,
            message: '标签名称长度应在 1-20 个字符之间',
            trigger: 'blur'
        }
    ]
}

// 重置搜索条件
const reset = () => {
    pickDate.value = ''
    startDate.value = null
    endDate.value = null
    searchTagName.value = ''
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

// 处理标签关闭
const handleClose = (tag) => {
    dynamicTags.value.splice(dynamicTags.value.indexOf(tag), 1)
}

// 显示输入框
const showInput = () => {
    inputVisible.value = true
    nextTick(() => {
        if (InputRef.value) {
            InputRef.value.focus()
        }
    })
}

// 处理输入确认
const handleInputConfirm = () => {
    if (inputValue.value.trim()) {
        // 检查标签是否重复
        if (!dynamicTags.value.includes(inputValue.value.trim())) {
            dynamicTags.value.push(inputValue.value.trim())
        } else {
            ElMessage.warning('标签已存在，请输入新的标签名称')
        }
    }
    inputVisible.value = false
    inputValue.value = ''
}

// 新增标签提交
const addTagsSubmit = () => {
    form.tags = dynamicTags.value
    submitLoading.value = true
    addTags(form).then((e) => {
        if (e.code !== 200) {
            showMessage(e.message, 'warning', 'message')
            return
        }

        showMessage('添加成功', 'success', 'message')
        isTagPublishDialogShow.value = false
        dynamicTags.value = []
        getTableData()
    }).finally(() => {
        submitLoading.value = false
    })
}

// 编辑标签
const editTag = (row) => {
    editForm.id = row.id
    editForm.name = row.name
    isEditDialogShow.value = true
}

// 更新标签提交
const updateTagSubmit = () => {
    editSubmitLoading.value = true
    updateTag(editForm).then((e) => {
        if (e.code !== 200) {
            showMessage(e.message, 'warning', 'message')
            return
        }

        showMessage('更新成功', 'success', 'message')
        isEditDialogShow.value = false
        getTableData()
    }).finally(() => {
        editSubmitLoading.value = false
    })
}

// 获取分页数据
function getTableData() {
    tableLoading.value = true
    getTagPageList({ 
        current: current.value, 
        size: size.value, 
        startDate: startDate.value, 
        endDate: endDate.value, 
        tagName: searchTagName.value 
    })
        .then((res) => {
            if (res.code === API_STATUS.SUCCESS) {
                tableData.value = res.data.records
                current.value = res.data.current
                total.value = res.data.total
                size.value = res.data.size
                
                // 更新统计信息
                updateStatistics(res.data.records)
            }
        }).finally(() => {
            tableLoading.value = false
        })
}

// 更新统计信息
function updateStatistics(records) {
    statistics[0].value = total.value
    const usedTags = records.filter(tag => tag.usageCount > 0).length
    statistics[1].value = usedTags
    statistics[2].value = total.value - usedTags
}

// 初始化数据
getTableData()

// 处理分页大小变化
const handleSizeChange = (e) => {
    size.value = e
    getTableData()
}

// 处理选择变化
const handleSelectionChange = (val) => {
    selectedTags.value = val
}

// 单个删除标签
const deleteTagSubmit = (row) => {
    ElMessageBox.confirm(
        `是否确认要删除标签 "${row.name}"?`,
        '删除确认',
        {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning'
        }
    )
        .then(() => {
            deleteTag(row.id).then((e) => {
                if (e.code === 200) {
                    showMessage('删除成功', 'success')
                    getTableData()
                } else {
                    showMessage(e.message, 'warning')
                }
            })
        })
        .catch(() => {
            // 取消删除
        })
}

// 批量删除标签
const batchDeleteTags = () => {
    if (selectedTags.value.length === 0) {
        showMessage('请选择要删除的标签', 'warning')
        return
    }

    ElMessageBox.confirm(
        `是否确认要删除选中的 ${selectedTags.value.length} 个标签?`,
        '批量删除确认',
        {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning'
        }
    )
        .then(() => {
            const tagIds = selectedTags.value.map(tag => tag.id)
            // 这里需要调用批量删除接口，如果没有则循环调用单个删除
            // 假设批量删除接口为 deleteBatchTags
            // deleteBatchTags({ ids: tagIds }).then((e) => {
            //     if (e.success == true) {
            //         showMessage('批量删除成功', 'success')
            //         getTableData()
            //     } else {
            //         showMessage(e.message, 'warning')
            //     }
            // })
            
            // 临时方案：循环删除
            let deletedCount = 0
            tagIds.forEach(id => {
                deleteTag(id).then((e) => {
                    if (e.code === 200) {
                        deletedCount++
                        if (deletedCount === tagIds.length) {
                            showMessage('批量删除成功', 'success')
                            getTableData()
                        }
                    }
                })
            })
        })
        .catch(() => {
            // 取消删除
        })
}

</script>

<style scoped>
.tag-management-container {
    padding: 20px;
}

.search-and-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 15px;
}

.search-section {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
}

.action-buttons {
    display: flex;
    gap: 10px;
}

.stats-section {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
}

.tags-container {
    min-height: 40px;
}

.button-new-tag {
    transition: all 0.3s ease;
}

.button-new-tag:hover {
    transform: translateY(-1px);
}

@media (max-width: 768px) {
    .search-and-actions {
        flex-direction: column;
        align-items: stretch;
    }

    .search-section {
        justify-content: stretch;
    }

    .action-buttons {
        justify-content: center;
    }

    .stats-section {
        justify-content: center;
    }

    .el-table {
        font-size: 14px;
    }

    .el-table-column {
        width: auto !important;
    }
}
</style>
