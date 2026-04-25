<template>
    <div :class="['flex gap-3 agent-msg-enter', message.role === 'user' ? 'justify-end' : 'justify-start']">
        <div v-if="message.role !== 'user'" class="w-8 h-8 rounded-lg bg-[var(--agent-primary)] flex items-center justify-center flex-shrink-0">
            <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
            </svg>
        </div>

        <div :class="['max-w-[var(--agent-msg-max-width)]', message.role === 'user' ? 'agent-msg-user px-4 py-3' : 'agent-msg-ai px-4 py-3']">
            <template v-if="message.role === 'user'">
                <p class="whitespace-pre-wrap text-sm leading-relaxed m-0">{{ message.content }}</p>
            </template>
            <template v-else>
                <AgentThoughtProcess v-if="execution" :execution="execution" />
                <div v-if="message.isError" class="flex items-center gap-2 text-[var(--agent-error)]">
                    <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <p class="text-sm m-0">{{ message.content }}</p>
                </div>
                <div v-else class="relative">
                    <!-- eslint-disable-next-line vue/no-v-html -->
                    <!-- v-html: 内容已经过 DOMPurify sanitize 处理，安全 -->
                    <div class="markdown-content text-sm leading-relaxed" v-html="renderedContent"></div>
                    <span v-if="message.isStreaming" class="agent-cursor"></span>
                </div>
                <AgentMessageActions :message="message" @copy="$emit('copy', message.content)" @retry="$emit('retry', message.id)" />
            </template>
        </div>

        <div v-if="message.role === 'user'" class="w-8 h-8 rounded-lg bg-[var(--agent-success)] flex items-center justify-center flex-shrink-0">
            <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
            </svg>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import logger from '@/utils/logger'
import AgentThoughtProcess from './AgentThoughtProcess.vue'
import AgentMessageActions from './AgentMessageActions.vue'

const props = defineProps({
    message: { type: Object, required: true },
    execution: { type: Object, default: null }
})

defineEmits(['copy', 'retry'])

const renderedContent = computed(() => {
    if (!props.message.content) {return ''}
    try {
        const html = marked.parse(props.message.content)
        return DOMPurify.sanitize(html, {
            ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'code', 'pre', 'h1', 'h2', 'h3', 'h4', 'ul', 'ol', 'li', 'blockquote', 'a', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
            ALLOWED_ATTR: ['href', 'target', 'class']
        })
    } catch (e) { logger.error('Markdown渲染失败:', e); return props.message.content }
})
</script>

<style scoped>
.markdown-content :deep(pre) {
    background: rgba(10, 10, 11, 0.6);
    padding: 12px 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 8px 0;
    border: 1px solid rgba(99, 102, 241, 0.15);
}
.markdown-content :deep(code) {
    font-family: var(--agent-font-mono);
    font-size: 0.85em;
}
.markdown-content :deep(pre code) { background: transparent; padding: 0; }
.markdown-content :deep(p) { margin: 4px 0; }
.markdown-content :deep(ul), .markdown-content :deep(ol) { margin: 4px 0; padding-left: 1.25rem; }
.markdown-content :deep(blockquote) { background: rgba(99, 102, 241, 0.08); padding: 8px 12px; margin: 8px 0; color: var(--agent-text-muted); border-radius: 4px; }
.markdown-content :deep(a) { color: var(--agent-primary); text-decoration: none; }
.markdown-content :deep(a:hover) { text-decoration: underline; }
</style>
