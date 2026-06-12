<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden user-page-card">
            <div class="bg-primary-subtle border-b border-border-color p-6 card-header">
                <h1 class="text-2xl font-bold text-text-primary">我的标签</h1>
                <p class="text-text-secondary mt-2">管理您创建的标签</p>
            </div>
            <div class="p-6 card-body">
                <div class="flex justify-between items-center mb-6 user-page-mb-6">
                    <div>
                        <button
                            class="btn btn-primary px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                            @click="openCreateDialog"
                        >
                            <svg class="w-4 h-4 mr-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.394 2.08a1 1 0 0 0-.788 0l-7 3a1 1 0 0 0 0 1.84L5.25 8.051a.999.999 0 0 1 .356-.257l4-1.714a1 1 0 1 1 .788 1.838L7.667 9.088l1.94.831a1 1 0 0 0 .787 0l7-3a1 1 0 0 0 0-1.838l-7-3Z" />
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2 12a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v2Z" />
                            </svg>
                            创建标签
                        </button>
                    </div>
                </div>

                <div v-if="loading" class="user-page-loading">
                    <div class="user-page-loading-spinner"></div>
                </div>

                <div v-else class="space-y-4">
                    <div v-if="tags.length === 0" class="user-page-empty">
                        <svg class="user-page-empty-icon" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 0 1 0 2.828l-7 7a2 2 0 0 1-2.828 0l-7-7A1.994 1.994 0 0 1 3 11V7a4 4 0 0 1 4-4Z" />
                        </svg>
                        <h3 class="user-page-empty-title">暂无标签</h3>
                        <p class="user-page-empty-desc">点击上方按钮创建您的第一个标签</p>
                    </div>
                    <div v-for="tag in tags" :key="tag.id" class="border border-border-color rounded-lg p-4 hover:bg-primary-subtle transition-all duration-300 user-page-item">
                        <div class="flex justify-between items-start mb-3 user-page-mb-3">
                            <div class="flex-1">
                                <h3 class="font-medium text-text-primary mb-1 user-page-mb-2">{{ tag.name }}</h3>
                                <p class="text-sm text-text-secondary mb-2 user-page-mb-2">{{ tag.description || '无描述' }}</p>
                                <div class="flex justify-between items-center">
                                    <span class="text-xs text-text-tertiary bg-primary-subtle px-2 py-1 rounded-full">
                                        文章数: {{ tag.articleCount }}
                                    </span>
                                    <span class="text-xs text-text-secondary">
                                        创建于 {{ formatDate(tag.createdAt) }}
                                    </span>
                                </div>
                            </div>
                            <div class="flex gap-2 user-page-gap-2">
                                <button
                                    class="btn btn-outline btn-sm px-3 py-1 text-xs"
                                    @click="openEditDialog(tag)"
                                >
                                    编辑
                                </button>
                                <button
                                    class="btn btn-outline btn-sm px-3 py-1 text-xs text-danger-color"
                                    @click="handleDeleteTag(tag.id)"
                                >
                                    删除
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑标签' : '创建标签'" width="500px">
            <el-form :model="formData" label-width="80px">
                <el-form-item label="标签名称" required>
                    <el-input v-model="formData.name" placeholder="请输入标签名称" />
                </el-form-item>
                <el-form-item label="标签描述">
                    <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入标签描述（可选）" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import { confirmDelete, showSuccess } from '@/utils'
import { getUserTagList, createTag, updateTag, deleteTag as apiDeleteTag } from '@/api/frontend/user'
import { API_STATUS } from '@/composables/api'
import '@/assets/css/common-user-pages.css'

const tags = ref([])
const total = ref(0)
const loading = ref(false)
const submitLoading = ref(false)

const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const formData = ref({
    name: '',
    description: ''
})

const formatDate = (dateString) => {
    if (!dateString) {return '未知'}
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    })
}

const openCreateDialog = () => {
    isEditing.value = false
    editingId.value = null
    formData.value = { name: '', description: '' }
    dialogVisible.value = true
}

const openEditDialog = (tag) => {
    isEditing.value = true
    editingId.value = tag.id
    formData.value = { name: tag.name, description: tag.description || '' }
    dialogVisible.value = true
}

const handleSubmit = async () => {
    if (!formData.value.name || !formData.value.name.trim()) {
        ElMessage.error('标签名称不能为空')
        return
    }
    submitLoading.value = true
    try {
        if (isEditing.value) {
            await updateTag(editingId.value, {
                name: formData.value.name,
                description: formData.value.description
            })
            showSuccess('更新成功')
        } else {
            await createTag({
                name: formData.value.name,
                description: formData.value.description
            })
            showSuccess('创建成功')
        }
        dialogVisible.value = false
        fetchTags()
    } catch (err) {
        logger.error('操作失败:', err.message)
        ElMessage.error('操作失败，请稍后重试')
    } finally {
        submitLoading.value = false
    }
}

const fetchTags = async () => {
    try {
        loading.value = true
        const res = await getUserTagList()
        if (res.code === API_STATUS.SUCCESS && res.data) {
            tags.value = res.data || []
            total.value = tags.value.length
        }
    } catch (err) {
        logger.error('获取标签列表失败:', err.message)
        ElMessage.error('获取标签列表失败')
    } finally {
        loading.value = false
    }
}

const handleDeleteTag = async (id) => {
    const tag = tags.value.find(t => t.id === id)
    if (!tag) {return}
    try {
        await confirmDelete(tag.name, '标签')
        await apiDeleteTag(id)
        showSuccess('删除成功')
        fetchTags()
    } catch (err) {
        if (err !== 'cancel') {
            logger.error('删除标签失败:', err.message)
            ElMessage.error('删除标签失败')
        }
    }
}

onMounted(() => {
    fetchTags()
})
</script>

<style scoped>
/* 已使用公共样式文件 common-user-pages.css */
</style>
