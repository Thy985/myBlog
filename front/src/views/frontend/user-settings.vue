<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden">
            <!-- 页面标题 -->
            <div class="bg-primary-subtle border-b border-border-color p-6">
                <h1 class="text-2xl font-bold text-text-primary">个人设置</h1>
                <p class="text-text-secondary mt-2">管理您的个人信息和账户设置</p>
            </div>

            <!-- 设置表单 -->
            <div class="p-6">
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    <!-- 左侧导航 -->
                    <div class="lg:col-span-1">
                        <div class="sticky top-24 space-y-2">
                            <button
                                class="w-full flex items-center p-4 rounded-lg transition-all duration-300 hover:bg-primary-subtle"
                                :class="activeTab === 'profile' ? 'bg-primary-subtle text-primary-color font-medium' : 'text-text-secondary'"
                                @click="activeTab = 'profile'"
                            >
                                <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm-7 9a7 7 0 1 1 14 0H3Z" />
                                </svg>
                                个人资料
                            </button>
                            <button
                                class="w-full flex items-center p-4 rounded-lg transition-all duration-300 hover:bg-primary-subtle"
                                :class="activeTab === 'account' ? 'bg-primary-subtle text-primary-color font-medium' : 'text-text-secondary'"
                                @click="activeTab = 'account'"
                            >
                                <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 2a5 5 0 0 0-5 5v2a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5a2 2 0 0 0-2-2V7a5 5 0 0 0-5-5Z" />
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
                                </svg>
                                账户设置
                            </button>
                            <button
                                class="w-full flex items-center p-4 rounded-lg transition-all duration-300 hover:bg-primary-subtle"
                                :class="activeTab === 'privacy' ? 'bg-primary-subtle text-primary-color font-medium' : 'text-text-secondary'"
                                @click="activeTab = 'privacy'"
                            >
                                <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 12.5V5.828M5 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2Z" />
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m10 14 2 2m0-4 2 2m0-4-2 2m-2 2 2 2" />
                                </svg>
                                隐私设置
                            </button>
                            <button
                                class="w-full flex items-center p-4 rounded-lg transition-all duration-300 hover:bg-primary-subtle"
                                :class="activeTab === 'security' ? 'bg-primary-subtle text-primary-color font-medium' : 'text-text-secondary'"
                                @click="activeTab = 'security'"
                            >
                                <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 12.5V5.828M5 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2Z" />
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
                                </svg>
                                安全设置
                            </button>
                            <button
                                class="w-full flex items-center p-4 rounded-lg transition-all duration-300 hover:bg-primary-subtle"
                                :class="activeTab === 'ai' ? 'bg-primary-subtle text-primary-color font-medium' : 'text-text-secondary'"
                                @click="activeTab = 'ai'"
                            >
                                <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                                </svg>
                                AI 配置
                            </button>
                        </div>
                    </div>

                    <!-- 右侧内容 -->
                    <div class="lg:col-span-2">
                        <!-- 个人资料设置 -->
                        <div v-if="activeTab === 'profile'" class="space-y-6">
                            <ProfileSettings
                                :username="profileForm.username"
                                :email="profileForm.email"
                                :bio="profileForm.bio"
                                :loading="loading"
                                @update="handleProfileUpdate"
                            />
                            <AvatarSettings
                                :username="store.user.username"
                                :avatar="store.user.avatar"
                                @upload="handleAvatarUpload"
                            />
                        </div>

                        <!-- 账户设置 -->
                        <div v-if="activeTab === 'account'" class="space-y-6">
                            <PasswordSettings
                                :loading="loading"
                                @update="handlePasswordUpdate"
                            />
                        </div>

                        <!-- 隐私设置 -->
                        <div v-if="activeTab === 'privacy'" class="space-y-6">
                            <PrivacySettings
                                :loading="loading"
                                @update="handlePrivacyUpdate"
                            />
                        </div>

                        <!-- 安全设置 -->
                        <div v-if="activeTab === 'security'" class="space-y-6">
                            <SecuritySettings
                                :loading="loading"
                                :mfa-enabled="securityForm.mfaEnabled"
                                :mfa-type="securityForm.mfaType"
                                :mfa-secret="securityForm.mfaSecret"
                                :backup-codes="backupCodes"
                                :username="store.user.username"
                                @save-mfa="handleMfaSave"
                                @generate-secret="handleGenerateSecret"
                                @verify-code="handleVerifyCode"
                                @generate-backup-codes="handleGenerateBackupCodes"
                                @regenerate-backup-codes="handleRegenerateBackupCodes"
                            />
                        </div>

                        <!-- AI 配置 -->
                        <div v-if="activeTab === 'ai'" class="space-y-6">
                            <AISettings
                                :loading="loading"
                                @save="handleAiSave"
                                @test-connection="handleTestConnection"
                            />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { showMessage } from '@/utils'
import logger from '@/utils/logger'
import {
  updateProfile,
  changePassword,
  updatePrivacy,
  uploadAvatar,
  updateMfaSetting,
  verifyMfaCode,
  generateBackupCodes,
  testAiConnection as testAiConnectionApi,
  saveAiConfig as saveAiConfigApi
} from '@/api/frontend/user'
import ProfileSettings from '@/components/common/ProfileSettings.vue'
import AvatarSettings from '@/components/common/AvatarSettings.vue'
import PasswordSettings from '@/components/common/PasswordSettings.vue'
import PrivacySettings from '@/components/common/PrivacySettings.vue'
import SecuritySettings from '@/components/common/SecuritySettings.vue'
import AISettings from '@/components/common/AISettings.vue'

const store = useAuthStore()

// 响应式数据
const activeTab = ref('profile')
const loading = ref(false)

// 表单数据
const profileForm = ref({
  username: '',
  email: '',
  bio: ''
})

const securityForm = ref({
  mfaEnabled: false,
  mfaType: 'sms',
  mfaSecret: '',
  mfaCode: ''
})

const backupCodes = ref([])

// 初始化表单数据
const initFormData = () => {
  if (store.isLoggedIn()) {
    profileForm.value.username = store.user.username || ''
    profileForm.value.email = store.user.email || ''
    profileForm.value.bio = store.user.bio || ''
  }
}

// 更新个人资料
const handleProfileUpdate = async () => {
  loading.value = true
  try {
    await updateProfile({
      username: profileForm.value.username,
      email: profileForm.value.email,
      bio: profileForm.value.bio
    })
    showMessage('个人资料更新成功', 'success')
  } catch (error) {
    logger.error('更新个人资料失败:', error)
    showMessage('更新失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

// 更新密码
const handlePasswordUpdate = async (formData) => {
  loading.value = true
  try {
    await changePassword({
      oldPassword: formData?.oldPassword || '',
      newPassword: formData?.newPassword || ''
    })
    showMessage('密码更新成功', 'success')
  } catch (error) {
    logger.error('修改密码失败:', error)
    showMessage('更新失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

// 更新隐私设置
const handlePrivacyUpdate = async (formData) => {
  loading.value = true
  try {
    await updatePrivacy(formData || {})
    showMessage('隐私设置更新成功', 'success')
  } catch (error) {
    logger.error('更新隐私设置失败:', error)
    showMessage('更新失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

// 处理头像上传
const handleAvatarUpload = async (file) => {
  loading.value = true
  try {
    const formData = new FormData()
    formData.append('avatar', file)
    const res = await uploadAvatar(formData)
    if (store.user) {
      store.setUser({ ...store.user, avatar: res.data?.url || '' })
    }
    showMessage('头像上传成功', 'success')
  } catch (error) {
    logger.error('上传头像失败:', error)
    showMessage('上传失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

// MFA相关方法
const handleMfaSave = async () => {
  loading.value = true
  try {
    await updateMfaSetting({
      mfaEnabled: securityForm.value.mfaEnabled,
      mfaType: securityForm.value.mfaType,
      mfaSecret: securityForm.value.mfaSecret
    })
    showMessage('MFA设置更新成功', 'success')
  } catch (error) {
    logger.error('保存MFA设置失败:', error)
    showMessage('更新设置失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

const handleGenerateSecret = async () => {
  loading.value = true
  try {
    const array = new Uint8Array(16)
    crypto.getRandomValues(array)
    const secret = Array.from(array, b => b.toString(36).padStart(2, '0')).join('').substring(0, 16).toUpperCase()
    securityForm.value.mfaSecret = secret
    showMessage('密钥生成成功，请妥善保管', 'success')
  } catch (error) {
    logger.error('生成密钥失败:', error)
    showMessage('生成密钥失败', 'error')
  } finally {
    loading.value = false
  }
}

const handleVerifyCode = async (code) => {
  if (!code) {
    showMessage('请输入验证码', 'warning')
    return
  }
  loading.value = true
  try {
    await verifyMfaCode({ code, mfaType: securityForm.value.mfaType })
    showMessage('验证成功', 'success')
    await handleMfaSave()
  } catch (error) {
    logger.error('验证MFA码失败:', error)
    showMessage('验证失败，请检查验证码是否正确', 'error')
  } finally {
    loading.value = false
  }
}

const handleGenerateBackupCodes = async () => {
  loading.value = true
  try {
    const res = await generateBackupCodes()
    backupCodes.value = res.data?.codes || []
    showMessage('备用验证码生成成功，请妥善保存', 'success')
  } catch (error) {
    logger.error('生成备用验证码失败:', error)
    showMessage('生成备用验证码失败', 'error')
  } finally {
    loading.value = false
  }
}

// AI 配置相关方法
const handleTestConnection = async (formData) => {
  loading.value = true
  try {
    await testAiConnectionApi(formData || {})
    showMessage('连接测试成功！', 'success')
  } catch (error) {
    logger.error('AI连接测试失败:', error)
    showMessage('连接测试失败，请检查配置', 'error')
  } finally {
    loading.value = false
  }
}

const handleAiSave = async (formData) => {
  loading.value = true
  try {
    await saveAiConfigApi(formData || {})
    showMessage('AI 配置保存成功', 'success')
  } catch (error) {
    logger.error('保存AI配置失败:', error)
    showMessage('保存失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

// 生命周期钩子
onMounted(() => {
  initFormData()
  setTimeout(() => {
    securityForm.value.mfaEnabled = false
    securityForm.value.mfaType = 'sms'
  }, 500)
})
</script>

<style scoped>
/* 响应式调整 */
@media (max-width: 1024px) {
  .lg\:col-span-1 {
    flex-shrink: 0;
  }

  .lg\:col-span-2 {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .grid-cols-2 {
    grid-template-columns: 1fr;
  }

  .flex-col.md\:flex-row {
    flex-direction: column;
  }
}
</style>
