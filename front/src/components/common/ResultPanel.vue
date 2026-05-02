<template>
  <div class="bg-background-primary border border-border-color rounded-lg p-4 max-h-80 overflow-y-auto">
    <div v-if="lines.length === 1" class="text-sm text-text-secondary">
      {{ lines[0] }}
    </div>
    <div v-else class="space-y-1">
      <div v-for="(line, idx) in lines" :key="idx" class="text-sm">
        <span v-if="isListItem(line)" class="flex gap-2">
          <span v-if="getListMarker(line)" class="text-[var(--color-primary)] font-medium">{{ getListMarker(line) }}</span>
          <span :class="getLineClass(line)">{{ getLineContent(line) }}</span>
        </span>
        <span v-else-if="isSectionHeader(line)" class="text-xs font-bold text-text-primary mt-2 block">{{ line }}</span>
        <span v-else class="text-text-secondary">{{ line }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  content: string
}>()

const lines = computed(() => props.content.split('\n').filter(l => l.trim()))

const isListItem = (line: string) => /^\d+\.|-/.test(line.trim())
const isSectionHeader = (line: string) => line.match(/^#{1,3}\s/) || line.match(/^[A-Z\u4E00-\u9FA5]{2,}:?$/)

const getListMarker = (line: string) => {
  const match = line.trim().match(/^(\d+\.|-)\s*/)
  return match ? match[1] : null
}

const getLineContent = (line: string) => {
  return line.replace(/^(\d+\.|-)\s*/, '')
}

const getLineClass = (line: string) => {
  const content = getLineContent(line).toLowerCase()
  if (content.includes('失败') || content.includes('错误')) return 'text-red-500'
  if (content.includes('成功') || content.includes('完成')) return 'text-green-600'
  if (content.includes('新增') || content.includes('优化')) return 'text-blue-600'
  return 'text-text-secondary'
}
</script>