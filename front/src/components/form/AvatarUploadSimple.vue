<template>
    <div class="avatar-upload-simple">
        <!-- 头像预览 -->
        <div class="avatar-preview">
            <img 
                v-if="currentAvatar" 
                :src="currentAvatar" 
                alt="头像" 
                class="avatar-image"
                loading="lazy"
                width="120"
                height="120"
                @error="(e) => e.target.style.display='none'"
            />
            <div v-else class="avatar-placeholder">
                <el-icon class="avatar-icon"><User /></el-icon>
            </div>
        </div>

        <!-- 上传按钮 -->
        <div class="upload-actions">
            <input
                ref="fileInputRef"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleFileSelect"
            />
            
            <el-button 
                type="primary" 
                size="small" 
                :loading="uploading"
                @click="selectFile"
            >
                <el-icon class="mr-1"><Upload /></el-icon>
                {{ uploading ? '上传中...' : '选择图片' }}
            </el-button>

            <el-button 
                v-if="currentAvatar" 
                size="small" 
                :disabled="uploading"
                @click="removeAvatar"
            >
                移除头像
            </el-button>
        </div>

        <!-- 提示信息 -->
        <div class="upload-tips">
            <p class="text-sm text-gray-500">
                支持 JPG、PNG、GIF 格式，文件大小不超过 2MB
            </p>
            <p class="text-sm text-gray-500">
                图片会自动裁剪为正方形并压缩
            </p>
        </div>
    </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Upload } from '@element-plus/icons-vue'
import request from '@/axios'
import logger from '@/utils/logger'
import { API_STATUS } from '@/constants'

const props = defineProps({
    modelValue: {
        type: String,
        default: ''
    },
    uploadUrl: {
        type: String,
        default: '/user/avatar/upload'
    }
})

const emit = defineEmits(['update:modelValue', 'change'])

// 数据
const currentAvatar = ref(props.modelValue)
const uploading = ref(false)
const fileInputRef = ref(null)

// 监听props变化
watch(() => props.modelValue, (newVal) => {
    currentAvatar.value = newVal
})

// 选择文件
const selectFile = () => {
    fileInputRef.value.click()
}

// 处理文件选择
const handleFileSelect = async (event) => {
    const file = event.target.files[0]
    if (!file) {return}

    // 验证文件类型
    if (!file.type.startsWith('image/')) {
        ElMessage.error('只能上传图片文件！')
        return
    }

    // 验证文件大小
    if (file.size > 2 * 1024 * 1024) {
        ElMessage.error('图片大小不能超过 2MB！')
        return
    }

    // 处理并上传图片
    await processAndUpload(file)

    // 清空input
    event.target.value = ''
}

// 处理并上传图片
const processAndUpload = async (file) => {
    try {
        uploading.value = true

        // 读取图片
        const image = await loadImage(file)

        // 裁剪为正方形
        const canvas = document.createElement('canvas')
        const size = 400
        canvas.width = size
        canvas.height = size
        const ctx = canvas.getContext('2d')

        // 计算裁剪区域
        const sourceSize = Math.min(image.width, image.height)
        const sourceX = (image.width - sourceSize) / 2
        const sourceY = (image.height - sourceSize) / 2

        // 绘制裁剪后的图片
        ctx.drawImage(
            image,
            sourceX, sourceY, sourceSize, sourceSize,
            0, 0, size, size
        )

        // 转换为Blob
        const blob = await new Promise(resolve => {
            canvas.toBlob(resolve, 'image/jpeg', 0.75)
        })

        // 上传
        await uploadAvatar(blob)

    } catch (error) {
        logger.error('处理图片失败:', error)
        ElMessage.error('处理图片失败，请重试')
    } finally {
        uploading.value = false
    }
}

// 加载图片
const loadImage = (file) => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = (e) => {
            const img = new Image()
            img.onload = () => resolve(img)
            img.onerror = reject
            img.src = e.target.result
        }
        reader.onerror = reject
        reader.readAsDataURL(file)
    })
}

// 上传头像
const uploadAvatar = async (blob) => {
    try {
        const formData = new FormData()
        formData.append('avatar', blob, 'avatar.jpg')

        const data = await request.post(props.uploadUrl, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })

        if (data.code === API_STATUS.SUCCESS) {
            currentAvatar.value = data.data.avatarUrl
            emit('update:modelValue', data.data.avatarUrl)
            emit('change', data.data.avatarUrl)
            ElMessage.success('头像上传成功！')
        } else {
            ElMessage.error(data.message || '上传失败')
        }
    } catch (error) {
        logger.error('上传失败:', error)
        ElMessage.error('上传失败，请重试')
    }
}

// 移除头像
const removeAvatar = () => {
    currentAvatar.value = ''
    emit('update:modelValue', '')
    emit('change', '')
    ElMessage.success('头像已移除')
}
</script>

<style scoped>
.avatar-upload-simple {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
    padding: 1rem;
}

.avatar-preview {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    overflow: hidden;
    border: 2px solid var(--border-color);
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--bg-tertiary);
}

.avatar-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.avatar-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--bg-tertiary);
}

.avatar-icon {
    font-size: 48px;
    color: var(--text-muted);
}

.upload-actions {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
    justify-content: center;
}

.upload-tips {
    text-align: center;
    max-width: 300px;
}

.text-sm {
    font-size: 0.875rem;
    line-height: 1.25rem;
}

.text-gray-500 {
    color: var(--text-muted);
}

.mr-1 {
    margin-right: 0.25rem;
}
</style>
