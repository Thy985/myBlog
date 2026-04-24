<template>
    <div class="container mx-auto px-4 py-6">
        <!-- 页面标题 -->
        <div class="mb-6">
            <h1 class="text-2xl font-bold text-gray-800 mb-2">媒体管理</h1>
            <p class="text-gray-600">管理和上传您的媒体文件</p>
        </div>

        <!-- 操作栏 -->
        <div class="media-container">
            <div class="flex items-center gap-4">
                <el-input v-model="searchQuery" placeholder="搜索文件名..." clearable style="width: 250px" />
                <el-select v-model="fileType" placeholder="文件类型" clearable style="width: 150px">
                    <el-option label="图片" value="image" />
                    <el-option label="视频" value="video" />
                    <el-option label="文档" value="document" />
                </el-select>
            </div>
            <div class="flex items-center gap-2">
                <el-button type="primary" :icon="Upload" @click="handleUpload">上传文件</el-button>
                <el-button :icon="Delete" :disabled="!selectedFiles.length">批量删除</el-button>
            </div>
        </div>

        <!-- 媒体网格 -->
        <div v-if="mediaList.length > 0" class="media-grid">
            <div
                v-for="item in mediaList"
                :key="item.id"
                class="media-card cursor-pointer"
                @click="handlePreview(item)"
            >
                <!-- 预览区域 -->
                <div class="media-preview">
                    <img v-if="item.type === 'image'" :src="item.url" :alt="item.name" loading="lazy" @error="(e) => e.target.style.display='none'" />
                    <div v-else class="flex items-center justify-center h-full text-text-muted">
                        <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                    </div>
                </div>

                <!-- 文件信息 -->
                <div class="media-info">
                    <p class="media-name">{{ item.name }}</p>
                    <p class="media-size">{{ formatFileSize(item.size) }}</p>
                </div>

                <!-- 选择框 -->
                <div class="absolute top-2 right-2">
                    <el-checkbox
                        :model-value="selectedFiles.includes(item.id)"
                        @change="(val) => handleSelect(item.id, val)"
                    />
                </div>
            </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="media-empty">
            <svg class="w-16 h-16 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <h3 class="media-empty-title">暂无文件</h3>
            <p class="media-empty-desc">点击上方按钮上传您的第一个文件</p>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="mt-6 flex justify-end">
            <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[12, 24, 48]"
                :total="total"
                layout="sizes, prev, pager, next"
                background
                @size-change="fetchMediaList"
                @current-change="fetchMediaList"
            />
        </div>

        <!-- 预览对话框 -->
        <el-dialog v-model="previewVisible" title="文件预览" width="600px">
            <div class="media-dialog-content">
                <img v-if="previewItem?.type === 'image'" :src="previewItem.url" :alt="previewItem.name" class="dialog-image" @error="(e) => { e.target.style.display='none'; e.target.nextElementSibling.style.display='flex' }" />
                <div v-if="previewItem?.type !== 'image'" class="text-center py-8 text-text-muted dialog-fallback" style="display:none">
                    <p>此文件类型不支持预览</p>
                </div>
            </div>
            <template #footer>
                <div class="media-dialog-footer">
                    <el-button @click="previewVisible = false">关闭</el-button>
                    <el-button type="primary" :icon="Download" @click="handleDownload(previewItem)">下载</el-button>
                    <el-button type="danger" :icon="Delete" @click="handleDelete(previewItem)">删除</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 上传对话框 -->
        <el-dialog v-model="uploadVisible" title="上传文件" width="500px">
            <el-upload
                ref="uploadRef"
                drag
                action="/api/media/upload"
                multiple
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :before-upload="beforeUpload"
                accept="image/*,video/*,.pdf,.doc,.docx"
            >
                <div class="py-8">
                    <el-icon class="w-16 h-16 text-text-muted mb-4"><UploadFilled /></el-icon>
                    <p class="text-sm text-text-secondary">将文件拖到此处，或<em>点击上传</em></p>
                    <p class="text-xs text-text-muted mt-2">支持图片、视频、PDF、Word 文档，单个文件不超过 50MB</p>
                </div>
            </el-upload>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete, Download, UploadFilled } from '@element-plus/icons-vue'
import logger from '@/utils/logger'

const searchQuery = ref('')
const fileType = ref('')
const mediaList = ref([])
const selectedFiles = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const previewVisible = ref(false)
const previewItem = ref(null)
const uploadVisible = ref(false)
const uploadRef = ref(null)

const fetchMediaList = async () => {
    try {
        const params = {
            page: currentPage.value,
            size: pageSize.value,
            keyword: searchQuery.value,
            type: fileType.value
        }
        const res = await fetch(`/api/media/list?${new URLSearchParams(params)}`)
        const data = await res.json()
        mediaList.value = data.records || []
        total.value = data.total || 0
    } catch (error) {
        logger.error('获取媒体列表失败', error)
        ElMessage.error('获取媒体列表失败')
    }
}

const handlePreview = (item) => {
    previewItem.value = item
    previewVisible.value = true
}

const handleSelect = (id, checked) => {
    if (checked) {
        selectedFiles.value.push(id)
    } else {
        const index = selectedFiles.value.indexOf(id)
        if (index > -1) {
            selectedFiles.value.splice(index, 1)
        }
    }
}

const handleUpload = () => {
    uploadVisible.value = true
}

const handleDownload = (item) => {
    if (!item) {return}
    window.open(item.url, '_blank')
}

const handleDelete = async (item) => {
    if (!item) {return}

    try {
        await ElMessageBox.confirm(
            `确定要删除文件 "${item.name}" 吗？`,
            '确认删除',
            { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
        )

        await fetch(`/api/media/${item.id}`, { method: 'DELETE' })
        ElMessage.success('删除成功')
        fetchMediaList()
        previewVisible.value = false
    } catch (error) {
        if (error !== 'cancel') {
            logger.error('删除失败', error)
            ElMessage.error('删除失败')
        }
    }
}

const beforeUpload = (file) => {
    const isLt50M = file.size / 1024 / 1024 < 50
    if (!isLt50M) {
        ElMessage.error('文件大小不能超过 50MB!')
        return false
    }
    return true
}

const handleUploadSuccess = (_response) => {
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fetchMediaList()
}

const handleUploadError = () => {
    ElMessage.error('上传失败，请重试')
}

const formatFileSize = (bytes) => {
    if (bytes === 0) {return '0 B'}
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(() => {
    fetchMediaList()
})
</script>

<style scoped>
/* 
 * 媒体管理页面样式
 * 使用 .media- 前缀避免与 Tailwind 工具类冲突
 * 所有样式使用设计系统变量，无 !important
 */

/* 布局容器 */
.media-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
    margin-bottom: 1rem;
}

/* 网格布局 */
.media-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 1rem;
}

@media (max-width: 1024px) {
    .media-grid {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }
}

@media (max-width: 768px) {
    .media-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 480px) {
    .media-grid {
        grid-template-columns: 1fr;
    }
}

/* 媒体卡片 */
.media-card {
    position: relative;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    padding: 0.75rem;
    transition: box-shadow var(--transition-fast), border-color var(--transition-fast);
    background: var(--bg-card);
}

.media-card:hover {
    box-shadow: var(--shadow-md);
    border-color: var(--color-primary-subtle);
}

/* 预览区域 */
.media-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
    height: 10rem;
    background-color: var(--bg-secondary);
    border-radius: var(--radius-sm);
    margin-bottom: 0.75rem;
    overflow: hidden;
}

.media-preview img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
}

/* 文件信息 */
.media-info {
    text-align: center;
}

.media-name {
    font-size: 0.875rem;
    color: var(--text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.media-size {
    font-size: 0.75rem;
    color: var(--text-muted);
    margin-top: 0.25rem;
}

/* 空状态 */
.media-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    padding-top: 3rem;
    padding-bottom: 3rem;
}

.media-empty-title {
    font-size: 2.25rem;
    font-weight: 700;
    color: var(--text-primary);
    margin-top: 0.75rem;
}

.media-empty-desc {
    font-size: 0.875rem;
    color: var(--text-muted);
    margin-top: 0.5rem;
}

/* 对话框 */
.media-dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    margin-top: 1rem;
}

.media-dialog-content {
    max-width: 100%;
    max-height: 500px;
    padding: 2rem;
    margin-top: 0.5rem;
}

.dialog-image {
    max-width: 100%;
    max-height: 450px;
    object-fit: contain;
    border-radius: var(--radius-md);
}
</style>
