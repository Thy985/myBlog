<template>
    <div class="avatar-upload">
        <div class="avatar-preview">
            <img 
                v-if="avatarUrl" 
                :src="avatarUrl" 
                alt="头像" 
                class="avatar-image"
            />
            <div v-else class="avatar-placeholder">
                <el-icon class="avatar-icon"><User /></el-icon>
            </div>
        </div>

        <div class="upload-actions">
            <el-upload
                ref="uploadRef"
                :action="uploadUrl"
                :show-file-list="false"
                :before-upload="beforeUpload"
                :on-success="handleSuccess"
                :on-error="handleError"
                accept="image/*"
            >
                <el-button type="primary" size="small">
                    <el-icon class="mr-1"><Upload /></el-icon>
                    选择图片
                </el-button>
            </el-upload>

            <el-button 
                v-if="avatarUrl" 
                size="small" 
                @click="removeAvatar"
            >
                移除头像
            </el-button>
        </div>

        <div class="upload-tips">
            <p class="text-sm text-gray-500">
                支持 JPG、PNG、GIF 格式，文件大小不超过 2MB
            </p>
            <p class="text-sm text-gray-500">
                建议上传正方形图片，系统会自动裁剪并压缩
            </p>
        </div>

        <!-- 图片裁剪对话框 -->
        <el-dialog 
            v-model="cropDialogVisible" 
            title="裁剪头像" 
            width="600px"
            :close-on-click-modal="false"
        >
            <div class="crop-container">
                <vue-cropper
                    ref="cropperRef"
                    :img="cropImageUrl"
                    :output-size="1"
                    :output-type="'jpeg'"
                    :info="true"
                    :full="false"
                    :can-move="true"
                    :can-move-box="true"
                    :fixed="true"
                    :fixed-number="[1, 1]"
                    :auto-crop="true"
                    :auto-crop-width="200"
                    :auto-crop-height="200"
                    :center-box="true"
                    :high="true"
                ></vue-cropper>
            </div>

            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="cropDialogVisible = false">取消</el-button>
                    <el-button type="primary" :loading="uploading" @click="confirmCrop">
                        确定上传
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Upload } from '@element-plus/icons-vue'
import VueCropper from 'vue-cropper'
import 'vue-cropper/dist/index.css'
import request from '@/axios'

import logger from '@/utils/logger'
const props = defineProps({
    modelValue: {
        type: String,
        default: ''
    }
})

const emit = defineEmits(['update:modelValue', 'change'])

// 数据
const avatarUrl = ref(props.modelValue)
const uploadUrl = '/user/avatar/upload'
const cropDialogVisible = ref(false)
const cropImageUrl = ref('')
const uploading = ref(false)
const uploadRef = ref(null)
const cropperRef = ref(null)

// 监听props变化
watch(() => props.modelValue, (newVal) => {
    avatarUrl.value = newVal
})

// 上传前验证
const beforeUpload = (file) => {
    // 验证文件类型
    const isImage = file.type.startsWith('image/')
    if (!isImage) {
        ElMessage.error('只能上传图片文件！')
        return false
    }

    // 验证文件大小
    const isLt2M = file.size / 1024 / 1024 < 2
    if (!isLt2M) {
        ElMessage.error('图片大小不能超过 2MB！')
        return false
    }

    // 显示裁剪对话框
    const reader = new FileReader()
    reader.onload = (e) => {
        cropImageUrl.value = e.target.result
        cropDialogVisible.value = true
    }
    reader.readAsDataURL(file)

    // 阻止自动上传
    return false
}

// 确认裁剪并上传
const confirmCrop = () => {
    uploading.value = true

    cropperRef.value.getCropBlob(async (blob) => {
        try {
            uploading.value = true
            const formData = new FormData()
            formData.append('file', blob, 'avatar.jpg')

            const data = await request.post(uploadUrl, formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            })

            if (data.code === 200) {
                avatarUrl.value = data.data.avatarUrl
                emit('update:modelValue', data.data.avatarUrl)
                emit('change', data.data.avatarUrl)
                ElMessage.success('头像上传成功！')
                cropDialogVisible.value = false
            } else {
                ElMessage.error(data.message || '上传失败')
            }
        } catch (error) {
            logger.error('上传失败:', error)
            ElMessage.error('上传失败，请重试')
        } finally {
            uploading.value = false
        }
    })
}

// 上传成功
const handleSuccess = (response) => {
    if (response.code === 200) {
        avatarUrl.value = response.data.avatarUrl
        emit('update:modelValue', response.data.avatarUrl)
        emit('change', response.data.avatarUrl)
        ElMessage.success('头像上传成功！')
    } else {
        ElMessage.error(response.message || '上传失败')
    }
}

// 上传失败
const handleError = (error) => {
    logger.error('上传失败:', error)
    ElMessage.error('上传失败，请重试')
}

// 移除头像
const removeAvatar = () => {
    avatarUrl.value = ''
    emit('update:modelValue', '')
    emit('change', '')
    ElMessage.success('头像已移除')
}

// 组件挂载时获取当前头像
onMounted(() => {
    if (!avatarUrl.value) {
        // 可以在这里调用API获取当前用户头像
        // fetchCurrentAvatar()
    }
})
</script>

<style scoped>
.avatar-upload {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
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
    background-color: #f3f4f6;
}

.avatar-icon {
    font-size: 48px;
    color: #9ca3af;
}

.upload-actions {
    display: flex;
    gap: 0.5rem;
}

.upload-tips {
    text-align: center;
}

.crop-container {
    width: 100%;
    height: 400px;
}

.mr-1 {
    margin-right: 0.25rem;
}
</style>
