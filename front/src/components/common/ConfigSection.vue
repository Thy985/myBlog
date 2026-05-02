<template>
  <div class="bg-[var(--bg-card)] rounded-xl p-6 shadow-sm">
    <div class="flex items-start gap-4">
      <div v-if="icon" class="shrink-0 w-12 h-12 rounded-xl flex items-center justify-center" :class="iconBgClass">
        <svg class="w-6 h-6" :class="iconClass" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path v-if="icon === 'cog'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
          <path v-if="icon === 'cog'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          <path v-if="icon === 'search'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          <path v-if="icon === 'lightning'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
      </div>
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 mb-1">
          <h2 class="text-lg font-bold text-text-primary">{{ title }}</h2>
          <span v-if="importance" class="px-2 py-0.5 text-xs font-medium rounded-full" :class="importanceClass">
            {{ importanceLabel }}
          </span>
        </div>
        <p v-if="description" class="text-text-secondary text-sm">{{ description }}</p>
      </div>
    </div>
    <div class="mt-4">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  title: string
  description?: string
  icon?: 'cog' | 'search' | 'lightning'
  importance?: 'high' | 'medium' | 'low'
}>()

const importanceLabel = computed(() => {
  switch (props.importance) {
    case 'high': return '核心'
    case 'medium': return '进阶'
    case 'low': return '可选'
    default: return ''
  }
})

const importanceClass = computed(() => {
  switch (props.importance) {
    case 'high': return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
    case 'medium': return 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
    case 'low': return 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
    default: return ''
  }
})

const iconBgClass = computed(() => {
  switch (props.icon) {
    case 'cog': return 'bg-blue-100 dark:bg-blue-900/30'
    case 'search': return 'bg-purple-100 dark:bg-purple-900/30'
    case 'lightning': return 'bg-amber-100 dark:bg-amber-900/30'
    default: return ''
  }
})

const iconClass = computed(() => {
  switch (props.icon) {
    case 'cog': return 'text-blue-600 dark:text-blue-400'
    case 'search': return 'text-purple-600 dark:text-purple-400'
    case 'lightning': return 'text-amber-600 dark:text-amber-400'
    default: return 'text-text-secondary'
  }
})
</script>