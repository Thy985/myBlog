<template>
  <div>
    <label class="block text-sm font-medium text-text-secondary mb-2">{{ label }}</label>
    <select
      :value="modelValue"
      class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <option v-for="opt in options" :key="opt.value" :value="opt.value">
        {{ opt.label }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
  label: string
  options: readonly { value: string; label: string }[]
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>