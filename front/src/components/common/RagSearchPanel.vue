<template>
  <div class="space-y-4">
    <div class="flex gap-3">
      <input
        :value="query"
        type="text"
        class="flex-1 px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
        placeholder="输入搜索关键词，如：Vue3 教程"
        @input="$emit('update:query', ($event.target as HTMLInputElement).value)"
        @keyup.enter="$emit('search')"
      >
      <select
        :value="topK"
        class="px-3 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] outline-none"
        @change="$emit('update:topK', Number(($event.target as HTMLSelectElement).value))"
      >
        <option :value="3">Top 3</option>
        <option :value="5">Top 5</option>
        <option :value="10">Top 10</option>
      </select>
      <button
        class="btn btn-primary px-4 py-2 text-sm shrink-0"
        :disabled="loading || !query.trim()"
        @click="$emit('search')"
      >
        <span v-if="loading" class="flex items-center gap-2">
          <span class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
          搜索中
        </span>
        <span v-else>搜索</span>
      </button>
    </div>

    <div v-if="results.length > 0" class="space-y-3">
      <div class="text-sm text-text-secondary">
        找到 <span class="text-[var(--color-primary)] font-medium">{{ results.length }}</span> 条相关内容
      </div>
      <div v-for="(result, idx) in results" :key="idx" class="border border-border-color rounded-lg p-4 bg-background-primary hover:border-[var(--color-primary)]/50 transition-colors">
        <div class="flex items-start justify-between gap-3 mb-2">
          <h4 class="font-medium text-text-primary text-sm">{{ result.title || '未命名文章' }}</h4>
          <span class="shrink-0 px-2 py-0.5 text-xs font-medium rounded-full" :class="scoreClass(result.score || 0)">
            {{ ((result.score || 0) * 100).toFixed(0) }}% 匹配
          </span>
        </div>
        <p class="text-sm text-text-secondary line-clamp-3">{{ result.snippet || result.content || result.text || '无内容摘要' }}</p>
      </div>
    </div>

    <div v-else-if="searched && !loading" class="text-center py-10">
      <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-[var(--color-primary-subtle)] flex items-center justify-center">
        <svg class="w-8 h-8 text-[var(--color-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <p class="text-text-secondary mb-2">未找到相关内容</p>
      <p class="text-sm text-text-tertiary">试试其他关键词，或检查 RAG 是否已正确配置</p>
    </div>

    <div v-else-if="!searched && !loading" class="text-center py-8">
      <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center">
        <svg class="w-8 h-8 text-text-tertiary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>
      <p class="text-text-secondary mb-1">搜索你的知识库</p>
      <p class="text-sm text-text-tertiary">输入关键词搜索相关文章内容</p>
    </div>

    <div v-if="error" class="p-3 rounded-lg bg-red-50 text-red-600 text-sm dark:bg-red-900/20">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RagResult } from '@/types/growth'

defineProps<{
  query: string
  topK: number
  results: RagResult[]
  searched: boolean
  error: string
  loading: boolean
}>()

defineEmits<{
  'update:query': [value: string]
  'update:topK': [value: number]
  'search': []
}>()

const scoreClass = (score: number) => {
  if (score >= 0.8) return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
  if (score >= 0.5) return 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
  return 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
}
</script>