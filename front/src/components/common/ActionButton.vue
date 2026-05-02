<template>
  <button
    class="group relative flex flex-col items-center justify-center p-4 border rounded-xl transition-all duration-200 disabled:opacity-50"
    :class="buttonClass"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <div v-if="loading" class="w-8 h-8 border-2 border-current border-t-transparent rounded-full animate-spin mb-2" :class="loadingClass"></div>
    <svg v-else class="mb-2" :class="iconSizeClass" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="iconPath" />
    </svg>
    <span class="font-medium text-sm">{{ label }}</span>
    <span v-if="description && !loading" class="absolute -bottom-6 left-1/2 -translate-x-1/2 text-xs whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity" :class="descriptionClass">
      {{ description }}
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  action: string
  loading: boolean
  icon: 'play' | 'document' | 'search' | 'lightbulb'
  label: string
  description?: string
  variant?: 'primary' | 'secondary' | 'danger'
  disabled: boolean
}>()

defineEmits<{
  click: []
}>()

const iconPaths = {
  play: 'M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
  document: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
  search: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z',
  lightbulb: 'M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z'
}

const iconPath = computed(() => iconPaths[props.icon])

const variantClasses = {
  primary: 'border-[var(--color-primary)] bg-[var(--color-primary-subtle)] hover:bg-[var(--color-primary)]/20 text-[var(--color-primary)]',
  secondary: 'border-border-color bg-background-primary hover:bg-[var(--color-primary-subtle)] hover:border-[var(--color-primary)] text-text-secondary hover:text-[var(--color-primary)]',
  danger: 'border-red-200 bg-red-50 hover:bg-red-100 text-red-600 dark:bg-red-900/20 dark:border-red-800 dark:hover:bg-red-900/30 dark:text-red-400'
}

const buttonClass = computed(() => variantClasses[props.variant || 'secondary'])
const loadingClass = computed(() => props.variant === 'primary' ? 'text-[var(--color-primary)]' : 'text-current')
const descriptionClass = computed(() => props.variant === 'primary' ? 'text-[var(--color-primary)]' : 'text-text-tertiary')
const iconSizeClass = computed(() => props.variant === 'primary' ? 'w-8 h-8 text-[var(--color-primary)]' : 'w-8 h-8')
</script>