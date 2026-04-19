<template>
    <div v-if="show" class="flex items-center gap-1 pt-2 mt-2 border-t border-[rgba(99,102,241,0.1)]">
        <button class="p-1.5 rounded-md hover:bg-[var(--agent-bg-hover)] transition-colors" title="复制" @click="$emit('copy')">
            <svg class="w-3.5 h-3.5 text-[var(--agent-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
            </svg>
        </button>
        <button v-if="canRetry" class="p-1.5 rounded-md hover:bg-[var(--agent-bg-hover)] transition-colors" title="重试" @click="$emit('retry')">
            <svg class="w-3.5 h-3.5 text-[var(--agent-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
        </button>
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    message: { type: Object, required: true }
})

defineEmits(['copy', 'retry'])

const show = computed(() => props.message.content && !props.message.isStreaming)
const canRetry = computed(() => props.message.role === 'assistant' && props.message.isError)
</script>
