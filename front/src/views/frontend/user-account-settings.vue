<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden">
            <div class="bg-primary-subtle border-b border-border-color p-6">
                <h1 class="text-2xl font-bold text-text-primary">账户设置</h1>
                <p class="text-text-secondary mt-2">管理您的账户安全</p>
            </div>
            <div class="p-6">
                <!-- 密码修改 -->
                <div class="mb-10">
                    <h2 class="text-lg font-medium text-text-primary mb-4">修改密码</h2>
                    <form class="space-y-4 max-w-md" @submit.prevent="changePassword">
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                当前密码
                            </label>
                            <input 
                                v-model="passwordForm.currentPassword"
                                type="password"
                                class="input input-outline w-full"
                                placeholder="请输入当前密码"
                            >
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                新密码
                            </label>
                            <input 
                                v-model="passwordForm.newPassword"
                                type="password"
                                class="input input-outline w-full"
                                placeholder="请输入新密码"
                            >
                            <p class="text-xs text-text-tertiary mt-1">
                                密码长度至少8位，包含字母和数字
                            </p>
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-text-primary mb-2">
                                确认新密码
                            </label>
                            <input 
                                v-model="passwordForm.confirmPassword"
                                type="password"
                                class="input input-outline w-full"
                                placeholder="请确认新密码"
                            >
                        </div>
                        <div class="flex justify-end">
                            <button 
                                type="submit"
                                class="btn btn-primary px-6 py-2"
                                :disabled="isChangingPassword"
                            >
                                {{ isChangingPassword ? '修改中...' : '修改密码' }}
                            </button>
                        </div>
                    </form>
                </div>
                
                <!-- 登录历史 -->
                <div>
                    <h2 class="text-lg font-medium text-text-primary mb-4">登录历史</h2>
                    <div class="space-y-4">
                        <div v-if="loginHistory.length === 0" class="py-8 text-center text-text-secondary">
                            暂无登录历史
                        </div>
                        <div v-for="item in loginHistory" :key="item.id" class="border border-border-color rounded-lg p-4 hover:bg-primary-subtle transition-all duration-300">
                            <div class="flex justify-between items-start mb-2">
                                <div>
                                    <h3 class="font-medium text-text-primary text-sm">{{ item.device }}</h3>
                                    <p class="text-xs text-text-secondary mt-1">
                                        {{ item.ipAddress }} · {{ formatDate(item.loginAt) }}
                                    </p>
                                </div>
                                <span 
                                    :class="[
                                        'px-2 py-1 rounded-full text-xs font-medium',
                                        item.status === 'active' ? 'bg-success-subtle text-success-color' : 'bg-text-tertiary text-text-secondary'
                                    ]"
                                >
                                    {{ item.status === 'active' ? '当前登录' : '已过期' }}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'

// 组件

// 密码修改表单
const passwordForm = ref({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
})

// 登录历史
const loginHistory = ref([])

// 提交状态
const isChangingPassword = ref(false)

// 格式化日期
const formatDate = (dateString) => {
    if (!dateString) {return '未知'}
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

// 修改密码
const changePassword = async () => {
    try {
        // 表单验证
        if (!passwordForm.value.currentPassword) {
            ElMessage.warning('请输入当前密码')
            return
        }
        if (!passwordForm.value.newPassword || passwordForm.value.newPassword.length < 8) {
            ElMessage.warning('新密码长度至少8位')
            return
        }
        if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
            ElMessage.warning('两次输入的密码不一致')
            return
        }

        isChangingPassword.value = true
        // TODO: 调用修改密码API
        logger.debug('修改密码操作')

        // 模拟修改成功
        setTimeout(() => {
            isChangingPassword.value = false
            // 重置表单
            passwordForm.value = {
                currentPassword: '',
                newPassword: '',
                confirmPassword: ''
            }
            ElMessage.success('密码修改成功')
        }, 1000)
    } catch (error) {
        isChangingPassword.value = false
        logger.error('修改密码失败:', error)
        ElMessage.error('修改密码失败，请重试')
    }
}

// 模拟获取登录历史
const fetchLoginHistory = () => {
    // 这里应该通过API获取真实数据
    loginHistory.value = [
        {
            id: 1,
            device: 'Windows 10 · Chrome 120.0',
            ipAddress: '192.168.1.1',
            loginAt: new Date().toISOString(),
            status: 'active'
        },
        {
            id: 2,
            device: 'MacOS · Safari 17.0',
            ipAddress: '192.168.1.2',
            loginAt: new Date(Date.now() - 86400000).toISOString(),
            status: 'expired'
        },
        {
            id: 3,
            device: 'iPhone · iOS 17.0',
            ipAddress: '192.168.1.3',
            loginAt: new Date(Date.now() - 172800000).toISOString(),
            status: 'expired'
        }
    ]
}

// 组件挂载时获取数据
onMounted(() => {
    fetchLoginHistory()
})
</script>

<style scoped>
/* 响应式调整 */
@media (max-width: 768px) {
    .p-6 {
        padding: 1rem;
    }
    
    .mb-10 {
        margin-bottom: 2.5rem;
    }
    
    .mb-4 {
        margin-bottom: 1rem;
    }
    
    .mb-2 {
        margin-bottom: 0.5rem;
    }
    
    .mt-1 {
        margin-top: 0.25rem;
    }
    
    .p-4 {
        padding: 0.75rem;
    }
    
    .py-8 {
        padding: 2rem 0;
    }
    
    .text-sm {
        font-size: 0.875rem;
    }
    
    .text-xs {
        font-size: 0.75rem;
    }
    
    .max-w-md {
        max-width: 100%;
    }
}
</style>
