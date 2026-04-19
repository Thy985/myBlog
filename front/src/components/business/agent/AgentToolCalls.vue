<template>
    <div v-if="toolCalls.length" class="space-y-2">
        <div
            v-for="tool in toolCalls"
            :key="tool.id"
            class="agent-tool"
        >
            <div class="agent-tool__header" @click="toggle(tool.id)">
                <div :class="['agent-tool__icon', iconClass(tool)]">
                    <svg v-if="isStatus(tool, 'executing')" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.196A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <svg v-else-if="isStatus(tool, 'completed')" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                    </svg>
                    <svg v-else-if="isStatus(tool, 'failed')" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                    <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    </svg>
                </div>
                <div class="flex-1 min-w-0">
                    <span class="text-sm font-medium text-[var(--agent-text)]">{{ formatName(tool.payload.name) }}</span>
                </div>
                <div class="flex items-center gap-2">
                    <span v-if="tool.meta?.executionTime" class="text-[11px] font-mono text-[var(--agent-text-muted)]">
                        {{ tool.meta.executionTime }}ms
                    </span>
                    <span :class="['agent-status', statusClass(tool)]">{{ statusLabel(tool) }}</span>
                    <svg :class="['w-4 h-4 agent-expand-icon', { 'agent-expand-icon--open': expanded.has(tool.id) }]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                    </svg>
                </div>
            </div>
            <div v-if="expanded.has(tool.id)" class="agent-tool__detail">
                <div class="mb-2">
                    <div class="text-[11px] text-[var(--agent-text-muted)] uppercase tracking-wide mb-1">参数</div>
                    <pre class="agent-tool__code agent-tool__code--params">{{ formatJson(tool.payload.input) }}</pre>
                </div>
                <div v-if="tool.result" class="mb-2">
                    <div class="text-[11px] text-[var(--agent-text-muted)] uppercase tracking-wide mb-1">结果</div>
                    <pre :class="['agent-tool__code', tool.result.success ? 'agent-tool__code--result' : 'agent-tool__code--error']">{{ formatJson(tool.result.output) }}</pre>
                </div>
                <div v-if="tool.result?.error" class="mb-2">
                    <div class="text-[11px] text-[var(--agent-text-muted)] uppercase tracking-wide mb-1">错误</div>
                    <pre class="agent-tool__code agent-tool__code--error">{{ tool.result.error }}</pre>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import logger from '@/utils/logger'

defineProps({
    toolCalls: { type: Array, default: () => [] }
})

const expanded = ref(new Set())

function toggle(id) {
    if (expanded.value.has(id)) {expanded.value.delete(id)}
    else {expanded.value.add(id)}
}

const NAME_MAP = {
    article_generator: '文章生成',
    web_search: '网络搜索',
    code_executor: '代码执行',
    publish_article: '发布文章',
    file_reader: '文件读取',
    database_query: '数据库查询'
}

function formatName(name) { return NAME_MAP[name] || name }

function isStatus(tool, status) {
    const s = tool.payload?.status
    if (status === 'completed') {return s === 'completed' || tool.result?.success}
    if (status === 'failed') {return s === 'failed' || (tool.result && !tool.result.success)}
    return s === status
}

function iconClass(tool) {
    if (isStatus(tool, 'executing')) {return 'agent-tool__icon--executing'}
    if (isStatus(tool, 'completed')) {return 'agent-tool__icon--completed'}
    if (isStatus(tool, 'failed')) {return 'agent-tool__icon--failed'}
    return 'agent-tool__icon--pending'
}

function statusClass(tool) {
    if (isStatus(tool, 'executing')) {return 'agent-status--executing'}
    if (isStatus(tool, 'completed')) {return 'agent-status--completed'}
    if (isStatus(tool, 'failed')) {return 'agent-status--failed'}
    return 'agent-status--pending'
}

function statusLabel(tool) {
    if (isStatus(tool, 'executing')) {return '执行中'}
    if (isStatus(tool, 'completed')) {return '成功'}
    if (isStatus(tool, 'failed')) {return '失败'}
    return '等待中'
}

function formatJson(obj) {
    if (!obj || (typeof obj === 'object' && Object.keys(obj).length === 0)) {return '无'}
    try { return JSON.stringify(obj, null, 2) } catch (e) { logger.debug('JSON序列化失败:', e); return String(obj) }
}
</script>
