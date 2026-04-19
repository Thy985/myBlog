<template>
    <div v-if="sources.length" class="agent-rag">
        <div class="agent-rag__header" @click="open = !open">
            <div class="flex items-center gap-2 text-sm font-medium text-[var(--agent-text)]">
                <svg class="w-4 h-4 text-[var(--agent-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path>
                </svg>
                <span>参考来源 ({{ sources.length }})</span>
            </div>
            <svg :class="['w-4 h-4 agent-expand-icon', { 'agent-expand-icon--open': open }]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
            </svg>
        </div>
        <div v-show="open" class="px-3 pb-3 space-y-2">
            <div
                v-for="(src, idx) in sources"
                :key="src.id || idx"
                :class="['agent-rag__item', { 'agent-rag__item--highlight': src.relevance >= 0.8 }]"
            >
                <div class="agent-rag__num">{{ idx + 1 }}</div>
                <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between gap-2 mb-0.5">
                        <a v-if="src.url" :href="src.url" target="_blank" rel="noopener" class="text-sm font-medium text-[#A5B4FC] hover:underline truncate">
                            {{ src.title }}
                        </a>
                        <span v-else class="text-sm font-medium text-[#A5B4FC] truncate">{{ src.title }}</span>
                        <span :class="relevanceClass(src.relevance)">{{ relevanceLabel(src.relevance) }}</span>
                    </div>
                    <p v-if="src.snippet" class="text-xs text-[var(--agent-text-muted)] line-clamp-2 m-0">{{ src.snippet }}</p>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
    sources: { type: Array, default: () => [] }
})

const open = ref(true)

function relevanceLabel(r) {
    if (r === undefined || r === null) {return '-'}
    return `${Math.round(r * 100)}%`
}

function relevanceClass(r) {
    const base = 'text-[11px] px-1.5 py-0.5 rounded font-medium flex-shrink-0'
    if (r >= 0.8) {return `${base} bg-[rgba(16,185,129,0.12)] text-[#4ADE80]`}
    if (r >= 0.5) {return `${base} bg-[rgba(234,179,8,0.12)] text-[#FACC15]`}
    return `${base} bg-[rgba(156,163,175,0.12)] text-[#9CA3AF]`
}
</script>
