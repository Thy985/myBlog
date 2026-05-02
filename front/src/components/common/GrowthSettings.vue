<template>
  <div class="space-y-6">
    <ConfigSection
      title="智能体配置"
      description="配置 Growth 智能体的基本行为"
      icon="cog"
      importance="high"
    >
      <div class="space-y-0">
        <ConfigSelect
          v-model="formData.growthCycle"
          label="执行周期"
          :options="GROWTH_CYCLE_OPTIONS"
        />

        <div class="my-4 px-4 py-3 bg-[var(--color-primary-subtle)] rounded-lg flex items-center gap-3">
          <svg class="w-5 h-5 text-[var(--color-primary)] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span class="text-sm text-text-secondary">
            下次执行：<span class="font-medium text-[var(--color-primary)]">{{ nextExecutionTime }}</span>
          </span>
        </div>

        <ConfigToggle
          v-for="option in TOGGLE_OPTIONS"
          :key="option.key"
          v-model="formData[option.key]"
          :label="option.label"
          :description="option.description"
          :importance="option.importance"
        />
      </div>
    </ConfigSection>

    <ConfigSection
      title="RAG 知识检索"
      description="搜索已索引的文章内容，验证 RAG 是否正常工作"
      icon="search"
      importance="medium"
    >
      <RagSearchPanel
        v-model:query="ragQuery"
        v-model:top-k="ragTopK"
        v-model:results="ragResults"
        v-model:searched="ragSearched"
        v-model:error="ragError"
        :loading="ragLoading"
        @search="handleRagSearch"
      />
    </ConfigSection>

    <ConfigSection
      title="快捷操作"
      description="手动触发 Growth 任务，查看执行结果"
      icon="lightning"
      importance="medium"
    >
      <QuickActionsPanel
        :loading="actionLoading"
        :result="actionResult"
        @execute="handleExecuteGrowth"
        @report="handleGetReport"
        @opportunities="handleDiscoverOpportunities"
        @topics="handleSuggestTopics"
        @clear-result="actionResult = null"
      />
    </ConfigSection>

    <div class="flex justify-end">
      <button
        class="btn btn-primary px-6 py-2.5 text-sm font-medium transition-all duration-300 hover:scale-105"
        :disabled="loading"
        @click="handleSave"
      >
        {{ loading ? '保存中...' : '保存配置' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  executeGrowthTask,
  getGrowthReport,
  discoverOpportunities,
  ragSearch,
  ragSuggestTopics
} from '@/api/frontend/growth'
import { showMessage } from '@/utils'
import { calculateNextExecutionTime } from '@/utils/format'
import type { GrowthFormData, ActionType, ActionResult } from '@/types/growth'
import { GROWTH_CYCLE_OPTIONS, TOGGLE_OPTIONS } from '@/types/growth'
import ConfigSection from './ConfigSection.vue'
import ConfigSelect from './ConfigSelect.vue'
import ConfigToggle from './ConfigToggle.vue'
import RagSearchPanel from './RagSearchPanel.vue'
import QuickActionsPanel from './QuickActionsPanel.vue'

defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits<{
  save: [data: GrowthFormData]
}>()

const formData = ref<GrowthFormData>({
  growthCycle: 'daily',
  autoExecute: true,
  contentAudit: true,
  seoOptimization: true,
  ragEnabled: true
})

const nextExecutionTime = computed(() => calculateNextExecutionTime(formData.value.growthCycle))

const ragQuery = ref('')
const ragTopK = ref(5)
const ragLoading = ref(false)
const ragResults = ref<any[]>([])
const ragSearched = ref(false)
const ragError = ref('')

const actionLoading = ref<ActionType>(null)
const actionResult = ref<ActionResult | null>(null)

const handleSave = () => {
  showMessage('配置已保存', 'success')
  emit('save', formData.value)
}

const handleApiError = (error: any, fallback: string) => {
  const message = error.response?.data?.message || error.message || fallback
  showMessage(message, 'error')
}

const extractData = (res: any) => res.data?.data ?? res.data

const handleRagSearch = async () => {
  if (!ragQuery.value.trim()) return
  ragLoading.value = true
  ragError.value = ''
  ragResults.value = []
  ragSearched.value = false

  try {
    const res = await ragSearch({ query: ragQuery.value, topK: ragTopK.value })
    ragResults.value = res.data?.data ?? res.data ?? []
    ragSearched.value = true
  } catch (error) {
    ragError.value = '搜索失败：' + (error.response?.data?.message || error.message)
  } finally {
    ragLoading.value = false
  }
}

const handleAction = async (type: Exclude<ActionType, null>, apiFn: () => Promise<any>) => {
  actionResult.value = null
  actionLoading.value = type
  try {
    const res = await apiFn()
    actionResult.value = formatResult(type, extractData(res))
  } catch (error) {
    handleApiError(error, '操作失败')
  } finally {
    actionLoading.value = null
  }
}

const formatResult = (type: string, data: any) => {
  const content = !data ? '无数据' : typeof data === 'string' ? data : JSON.stringify(data, null, 2)
  const titles: Record<string, string> = {
    execute: '执行结果',
    report: 'Growth 报告',
    opportunities: '发现的机会',
    topics: '推荐创作主题'
  }
  return { title: titles[type] || '结果', content }
}

const handleExecuteGrowth = () => handleAction('execute', () =>
  executeGrowthTask({ cycle: formData.value.growthCycle })
)

const handleGetReport = () => handleAction('report', () =>
  getGrowthReport(formData.value.growthCycle)
)

const handleDiscoverOpportunities = () => handleAction('opportunities', discoverOpportunities)

const handleSuggestTopics = () => handleAction('topics', () =>
  ragSuggestTopics({ limit: 10 })
)
</script>