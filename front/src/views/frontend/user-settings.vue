<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <!-- 页面标题 -->
        <div class="mb-6">
            <h1 class="text-2xl font-bold text-text-primary">个人设置</h1>
            <p class="text-text-secondary mt-1">管理您的个人信息和账户设置</p>
        </div>

        <!-- 设置表单 -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <!-- 左侧导航 -->
            <div class="lg:col-span-1">
                <div class="sticky top-24 space-y-2">
                    <button
                        class="w-full flex items-center p-3 rounded-lg transition-all duration-200 text-sm"
                        :class="activeTab === 'privacy' ? 'bg-[var(--color-primary-subtle)] text-[var(--color-primary)] font-medium border-l-2 border-[var(--color-primary)]' : 'text-text-secondary hover:bg-[var(--color-primary-subtle)] border-l-2 border-transparent'"
                        @click="activeTab = 'privacy'"
                    >
                        <svg class="w-4 h-4 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 12.5V5.828M5 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2Z" />
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m10 14 2 2m0-4 2 2m0-4-2 2m-2 2 2 2" />
                        </svg>
                        隐私设置
                    </button>
                    <button
                        class="w-full flex items-center p-3 rounded-lg transition-all duration-200 text-sm"
                        :class="activeTab === 'security' ? 'bg-[var(--color-primary-subtle)] text-[var(--color-primary)] font-medium border-l-2 border-[var(--color-primary)]' : 'text-text-secondary hover:bg-[var(--color-primary-subtle)] border-l-2 border-transparent'"
                        @click="activeTab = 'security'"
                    >
                        <svg class="w-4 h-4 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 12.5V5.828M5 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2Z" />
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
                        </svg>
                        安全设置
                    </button>
                    <button
                        class="w-full flex items-center p-3 rounded-lg transition-all duration-200 text-sm"
                        :class="activeTab === 'ai' ? 'bg-[var(--color-primary-subtle)] text-[var(--color-primary)] font-medium border-l-2 border-[var(--color-primary)]' : 'text-text-secondary hover:bg-[var(--color-primary-subtle)] border-l-2 border-transparent'"
                        @click="activeTab = 'ai'"
                    >
                        <svg class="w-4 h-4 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                        </svg>
                        AI 配置
                    </button>
                    <button
                        class="w-full flex items-center p-3 rounded-lg transition-all duration-200 text-sm"
                        :class="activeTab === 'agent' ? 'bg-[var(--color-primary-subtle)] text-[var(--color-primary)] font-medium border-l-2 border-[var(--color-primary)]' : 'text-text-secondary hover:bg-[var(--color-primary-subtle)] border-l-2 border-transparent'"
                        @click="activeTab = 'agent'"
                    >
                        <svg class="w-4 h-4 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                        </svg>
                        智能体设置
                    </button>
                </div>
            </div>

            <!-- 右侧内容 -->
            <div class="lg:col-span-2">
                <Transition name="tab-fade" mode="out-in">
                    <div :key="activeTab" class="space-y-8">
                        <template v-if="activeTab === 'privacy'">
                            <Suspense>
                                <template #default>
                                    <PrivacySettings
                                        :loading="loading"
                                        @update="handlePrivacyUpdate"
                                    />
                                </template>
                                <template #fallback>
                                    <div class="flex items-center justify-center py-12">
                                        <div class="w-8 h-8 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin"></div>
                                    </div>
                                </template>
                            </Suspense>
                        </template>

                        <template v-if="activeTab === 'security'">
                            <Suspense>
                                <template #default>
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
                                </template>
                                <template #fallback>
                                    <div class="flex items-center justify-center py-12">
                                        <div class="w-8 h-8 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin"></div>
                                    </div>
                                </template>
                            </Suspense>
                        </template>

                        <template v-if="activeTab === 'ai'">
                            <Suspense>
                                <template #default>
                                    <AISettings
                                        :loading="loading"
                                        :saved="aiSaved"
                                        @save="handleAiSave"
                                        @test-connection="handleTestConnection"
                                    />
                                </template>
                                <template #fallback>
                                    <div class="flex items-center justify-center py-12">
                                        <div class="w-8 h-8 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin"></div>
                                    </div>
                                </template>
                            </Suspense>
                        </template>

                        <template v-if="activeTab === 'agent'">
                            <Suspense>
                                <template #default>
                                    <GrowthSettings
                                        :loading="loading"
                                        @save="handleAgentSave"
                                    />
                                </template>
                                <template #fallback>
                                    <div class="flex items-center justify-center py-12">
                                        <div class="w-8 h-8 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin"></div>
                                    </div>
                                </template>
                            </Suspense>
                        </template>
                    </div>
                </Transition>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, defineAsyncComponent } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { showMessage } from '@/utils'
import logger from '@/utils/logger'
import {
  updatePrivacy,
  updateMfaSetting,
  verifyMfaCode,
  generateBackupCodes,
  testAiConnection as testAiConnectionApi,
  saveAiConfig as saveAiConfigApi
} from '@/api/frontend/user'
import { scheduleGrowthTask } from '@/api/frontend/growth'

// 懒加载设置组件
const PrivacySettings = defineAsyncComponent(() => import('@/components/common/PrivacySettings.vue'))
const SecuritySettings = defineAsyncComponent(() => import('@/components/common/SecuritySettings.vue'))
const AISettings = defineAsyncComponent(() => import('@/components/common/AISettings.vue'))
const GrowthSettings = defineAsyncComponent({
  loader: () => import('@/components/common/GrowthSettings.vue'),
  errorComponent: { template: '<div class="text-red-500 p-4">加载失败</div>' },
  delay: 200,
  timeout: 5000
})

const store = useAuthStore()

// 响应式数据
const activeTab = ref('privacy')
const loading = ref(false)
const aiSaved = ref(false)

const securityForm = ref({
  mfaEnabled: false,
  mfaType: 'sms',
  mfaSecret: '',
  mfaCode: ''
})

const backupCodes = ref([])

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
    const apiData = {
      provider: formData.llmProvider?.toUpperCase(),
      apiKey: formData.apiKey,
      defaultModel: formData.model
    }
    await testAiConnectionApi(apiData)
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
  aiSaved.value = false
  try {
    const apiData = {
      provider: formData.llmProvider?.toUpperCase(),
      apiKey: formData.apiKey,
      defaultModel: formData.model
    }
    await saveAiConfigApi(apiData)
    aiSaved.value = true
    showMessage('AI 配置保存成功', 'success')
  } catch (error) {
    logger.error('保存AI配置失败:', error)
    showMessage('保存失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

const handleAgentSave = async (formData) => {
  loading.value = true
  try {
    await scheduleGrowthTask({
      cycle: formData.growthCycle,
      options: {
        autoExecute: formData.autoExecute,
        contentAudit: formData.contentAudit,
        seoOptimization: formData.seoOptimization,
        ragEnabled: formData.ragEnabled
      }
    })
    showMessage('智能体设置保存成功', 'success')
  } catch (error) {
    logger.error('保存智能体设置失败:', error)
    showMessage('保存失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

// 生命周期钩子
onMounted(() => {
  setTimeout(() => {
    securityForm.value.mfaEnabled = false
    securityForm.value.mfaType = 'sms'
  }, 500)
})
</script>

<style scoped>
/* 标签页切换动画 */
.tab-fade-enter-active,
.tab-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.tab-fade-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.tab-fade-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .lg\:col-span-1 {
    flex-shrink: 0;
  }

  .lg\:col-span-2 {
    flex: 1;
  }

  /* 确保粘性侧边栏不会与内容重叠 */
  .lg\:col-span-1 {
    position: relative;
    top: 0;
  }
}

@media (max-width: 768px) {
  .grid-cols-2 {
    grid-template-columns: 1fr;
  }

  .flex-col.md\:flex-row {
    flex-direction: column;
  }

  /* 移动端禁用粘性定位避免覆盖内容 */
  .sticky {
    position: relative;
    top: 0;
  }

  /* 优化旋转动画性能 */
  .animate-spin {
    will-change: transform;
  }
}
</style>
