<template>
  <div>
    <div class="mb-3 flex items-center gap-2 text-sm text-text-secondary">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <span>推荐流程：先发现机会 → 查看报告 → 再执行</span>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <ActionButton
        action="opportunities"
        :loading="loading === 'opportunities'"
        icon="search"
        label="发现机会"
        description="AI 分析内容机会"
        variant="primary"
        :disabled="loading !== null"
        @click="$emit('opportunities')"
      />

      <ActionButton
        action="report"
        :loading="loading === 'report'"
        icon="document"
        label="查看报告"
        description="分析已生成的内容"
        variant="secondary"
        :disabled="loading !== null"
        @click="$emit('report')"
      />

      <ActionButton
        action="topics"
        :loading="loading === 'topics'"
        icon="lightbulb"
        label="推荐主题"
        description="获取创作灵感"
        variant="secondary"
        :disabled="loading !== null"
        @click="$emit('topics')"
      />

      <ActionButton
        action="execute"
        :loading="loading === 'execute'"
        icon="play"
        label="立即执行"
        description="手动触发 Growth"
        variant="danger"
        :disabled="loading !== null"
        @click="$emit('execute')"
      />
    </div>

    <div v-if="result" class="mt-4">
      <div class="border-t border-border-color pt-4">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 bg-green-500 rounded-full"></span>
            <h3 class="font-medium text-text-primary text-sm">{{ result.title }}</h3>
          </div>
          <button class="text-text-tertiary hover:text-text-primary transition-colors" @click="$emit('clearResult')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <ResultPanel :content="result.content" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ActionResult, ActionType } from '@/types/growth'
import ActionButton from './ActionButton.vue'
import ResultPanel from './ResultPanel.vue'

defineProps<{
  loading: ActionType
  result: ActionResult | null
}>()

defineEmits<{
  execute: []
  report: []
  opportunities: []
  topics: []
  clearResult: []
}>()
</script>