<template>
  <div class="flex items-center justify-between py-3 border-b border-border-color last:border-b-0">
    <div class="flex items-center gap-3">
      <div class="flex items-center gap-2">
        <h3 class="font-medium text-text-primary">{{ label }}</h3>
        <span v-if="importance === 'high'" class="px-1.5 py-0.5 text-xs font-medium bg-red-100 text-red-600 rounded dark:bg-red-900/30 dark:text-red-400">
          核心
        </span>
        <span v-else-if="importance === 'medium'" class="px-1.5 py-0.5 text-xs font-medium bg-yellow-100 text-yellow-600 rounded dark:bg-yellow-900/30 dark:text-yellow-400">
          进阶
        </span>
      </div>
      <p v-if="description" class="text-sm text-text-secondary">{{ description }}</p>
    </div>
    <label class="relative inline-flex items-center cursor-pointer">
      <input
        :checked="modelValue"
        type="checkbox"
        class="sr-only peer"
        @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
      >
      <div class="w-11 h-6 bg-[var(--toggle-track)] peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-[var(--toggle-border)] after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[var(--color-primary)]"></div>
    </label>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: boolean
  label: string
  description?: string
  importance?: 'high' | 'medium' | 'low'
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()
</script>