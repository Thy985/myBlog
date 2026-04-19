<template>
    <div v-if="intent" :class="['agent-intent', intentClass]">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="intentIcon"></path>
        </svg>
        <span>{{ intentLabel }}</span>
        <div
            :class="['agent-intent__confidence', confidenceClass]"
            :style="{ width: confidenceWidth }"
        ></div>
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    intent: { type: Object, required: true }
})

const INTENT_MAP = {
    CREATE_ARTICLE: { label: '创建文章', icon: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z', cls: 'create' },
    EDIT_ARTICLE: { label: '编辑文章', icon: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z', cls: 'edit' },
    SEARCH: { label: '搜索查询', icon: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z', cls: 'search' },
    CODE_GENERATE: { label: '代码生成', icon: 'M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4', cls: 'code' },
    SUMMARIZE: { label: '内容总结', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z', cls: 'default' },
    TRANSLATE: { label: '翻译', icon: 'M3 5h12M9 3v2m1.048 9.5A18.022 18.022 0 016.412 9m6.088 9h7M11 21l5-10 5 10M12.751 5C11.783 10.77 8.07 15.61 3 18.129', cls: 'default' },
    KNOWLEDGE_QUERY: { label: '知识查询', icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253', cls: 'default' },
    CHAT: { label: '普通对话', icon: 'M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z', cls: 'chat' }
}

const config = computed(() => INTENT_MAP[props.intent.type] || { label: props.intent.type, icon: 'M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z', cls: 'default' })

const intentClass = computed(() => `agent-intent--${config.value.cls}`)
const intentLabel = computed(() => config.value.label)
const intentIcon = computed(() => config.value.icon)

const confidenceWidth = computed(() => {
    const c = props.intent.confidence
    return c !== undefined && c !== null ? `${Math.round(c * 100)}%` : '0%'
})

const confidenceClass = computed(() => {
    const c = props.intent.confidence ?? 0
    if (c >= 0.8) {return 'agent-intent__confidence--high'}
    if (c >= 0.5) {return 'agent-intent__confidence--medium'}
    return 'agent-intent__confidence--low'
})
</script>
