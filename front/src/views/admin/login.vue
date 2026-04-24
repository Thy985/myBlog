<template>
    <div>
        <div class="grid grid-cols-6 h-screen bg-white">
            <!-- 左边栏 -->
            <div class="col-span-6 md:col-span-3 sm:col-span-6">
                <div class="login-container-left flex justify-center items-center flex-col">
                    <div class="animate__animated animate__bounceInLeft items-center flex flex-col">
                        <h2 class="font-bold text-4xl mb-7 text-white">星辰博客登录</h2>
                        <p class="text-white">走向星辰大海，致力未来远征</p>
                        <img src="@/assets/头像.jpg" class="login-image" loading="lazy" alt="登录页面插图">
                    </div>
                </div>
            </div>
            <!-- 右边栏 -->
            <div class="col-span-6 px-3 md:col-span-3 sm:col-span-6">
                <div
                    class="login-container-right flex justify-center items-center flex-col animate__animated animate__bounceInRight animate__fast">
                    <h2 class="font-bold text-3xl text-gray-800 mt-5">欢迎回来</h2>
                    <div class="flex items-center justify-center my-5 text-gray-400 space-x-2">
                        <span class="h-[1px] w-16 bg-gray-200"></span>
                        <span>账号密码登录</span>
                        <span class="h-[1px] w-16 bg-gray-200"></span>
                    </div>
                    <div>
                        <el-form ref="formRef" :rules="rules" :model="form" class="w-[300px]">
                            <el-form-item prop="username">
                                <el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名" size="large" clearable/>
                            </el-form-item>
                            <el-form-item prop="password">
                                <el-input
v-model="form.password" type="password" autocomplete="off" :prefix-icon="Lock"
                                    placeholder="请输入密码" show-password size="large" clearable/>
                            </el-form-item>
                            <el-form-item>

                                <el-button
round type="primary" :loading="loading" class="w-[300px] login-btn mt-4"
                                    size="large" @click="onSubmit">
                                    登 录
                                </el-button>

                            </el-form-item>
                        </el-form>
                    </div>

                </div>
            </div>
        </div>

    </div>
</template>
  
<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { login } from '@/api/admin/user'
import { showMessage } from '@/utils'
import { useRouter } from 'vue-router'
import { setToken, getRedirectUrl, clearRedirectUrl } from '@/composables/auth'
import { useAuthStore } from '@/stores/auth'
import { User, Lock } from '@element-plus/icons-vue'
import { API_STATUS } from '@/composables/api'
import logger from '@/utils/logger'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const authStore = useAuthStore()

const form = reactive({
    username: '',
    password: ''
})

const rules = {
    username: [
        {
            required: true,
            message: '用户名不能为空',
            trigger: 'blur'
        }
    ],
    password: [
        {
            required: true,
            message: '密码不能为空',
            trigger: 'blur'
        }
    ]
}

const onSubmit = () => {
    // 登录表单验证
    formRef.value.validate(async (valid) => {
        if (!valid) {
            return false
        }
        loading.value = true
        try {
            const res = await login(form.username, form.password)
            if (res.code === API_STATUS.SUCCESS) {
                showMessage('登录成功', 'success')
                setToken(res.data.token)
                authStore.setUser(res.data.user || {})

                try {
                    await authStore.getAdminInfo()
                } catch (error) {
                    logger.error('获取用户信息失败:', error)
                }

                const redirectUrl = getRedirectUrl()
                if (redirectUrl !== '/login' && redirectUrl !== '/admin/login') {
                    clearRedirectUrl()
                    router.push(redirectUrl)
                } else {
                    router.push('/admin')
                }
            } else {
                showMessage(res.message, 'error')
            }
        } catch (error) {
            if (error.response) {
                switch (error.response.status) {
                    case 401:
                        showMessage('账号或密码错误，请重新输入', 'error')
                        break
                    case 403:
                        showMessage('账号已被禁用，请联系管理员', 'error')
                        break
                    case 429:
                        showMessage('登录过于频繁，请稍后重试', 'error')
                        break
                    default:
                        showMessage('登录失败，请稍后重试', 'error')
                }
            } else if (error.request) {
                showMessage('网络连接失败，请检查网络设置', 'error')
            } else {
                showMessage('登录失败，请稍后重试', 'error')
            }
            logger.error('登录失败:', error)
        } finally {
            loading.value = false
        }
    })
}

function onKeyUp(e) {
    if (e.key === 'Enter') {
        onSubmit()
    }
}

onMounted(() => {
    document.addEventListener('keyup', onKeyUp)
})

// 移除键盘监听
onBeforeUnmount(() => {
    document.removeEventListener('keyup', onKeyUp)
})

</script>

<style scoped>
:deep([type='text']:focus) {
    border-color: transparent !important;
}

.login-container {
    height: 100vh;
    width: 100%;
    background-color: var(--bg-primary, #ffffff);
}

.login-container-left {
    height: 100%;
    background: linear-gradient(135deg, var(--color-primary, #6366F1) 0%, #1e1b4b 100%);
    color: var(--text-primary, #ffffff);
}

.dark .login-container-left {
    background: linear-gradient(135deg, #1e1b4b 0%, #0f0f23 100%);
}

.login-container-right {
    height: 100%;
    background-color: var(--bg-primary, #ffffff);
}

.dark .login-container-right {
    background-color: var(--bg-primary, #0A0A0B);
}

.login-image {
    height: clamp(250px, 40vh, 450px);
    max-width: 100%;
    object-fit: contain;
}

/* 响应式优化 */
@media (max-width: 768px) {
    .login-container-left {
        min-height: 200px;
        padding: 2rem 1rem;
    }
    
    .login-container-left h2 {
        font-size: 1.5rem;
    }
    
    .login-image {
        height: 150px;
    }
}

.login-btn {
    transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.login-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px var(--color-primary-glow, rgba(99, 102, 241, 0.4));
}
</style>
