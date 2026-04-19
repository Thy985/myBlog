<template>
    <div v-if="hasContent" class="agent-thought-panel mt-2">
        <div class="agent-thought-header" @click="open = !open">
            <div class="flex items-center gap-2">
                <svg class="w-3.5 h-3.5 text-[var(--agent-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
                </svg>
                <span class="text-xs font-medium text-[var(--agent-text)]">思考过程</span>
                <span v-if="intent" class="text-[11px] text-[var(--agent-text-muted)]">· {{ intentLabel }}</span>
            </div>
            <svg :class="['w-4 h-4 agent-expand-icon', { 'agent-expand-icon--open': open }]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
            </svg>
        </div>
        <div v-show="open" class="agent-thought-body space-y-3">
            <AgentIntentCard v-if="intent" :intent="intent" />
            <AgentPlanProgress v-if="plan" :plan="plan" :progress="progress" />
            <AgentToolCalls :tool-calls="toolCalls" />
            <AgentRagSources :sources="ragSources" />
        </div>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { AgentEventType, ThoughtSubType, ActionSubType, ObservationSubType } from '@/types/agent'
import AgentIntentCard from './AgentIntentCard.vue'
import AgentPlanProgress from './AgentPlanProgress.vue'
import AgentToolCalls from './AgentToolCalls.vue'
import AgentRagSources from './AgentRagSources.vue'

const INTENT_LABELS = {
    CREATE_ARTICLE: '创建文章', EDIT_ARTICLE: '编辑文章', SEARCH: '搜索',
    CODE_GENERATE: '代码生成', CHAT: '对话', KNOWLEDGE_QUERY: '知识查询'
}

const props = defineProps({
    execution: { type: Object, required: true }
})

const open = ref(true)

const events = computed(() => props.execution?.events || [])

const hasContent = computed(() => events.value.some(e =>
    e.type === AgentEventType.THOUGHT || e.type === AgentEventType.ACTION || e.type === AgentEventType.OBSERVATION
))

const intent = computed(() => {
    const ev = events.value.find(e => e.type === AgentEventType.THOUGHT && e.subType === ThoughtSubType.INTENT)
    return ev?.payload?.data
})

const intentLabel = computed(() => INTENT_LABELS[intent.value?.type] || intent.value?.type || '')

const plan = computed(() => {
    const ev = events.value.find(e => e.type === AgentEventType.THOUGHT && e.subType === ThoughtSubType.PLAN)
    return ev?.payload?.data
})

const progress = computed(() => {
    const total = plan.value?.steps?.length || 0
    const current = events.value.filter(e => e.type === AgentEventType.ACTION && e.payload?.status === 'completed').length
    return { current, total, percentage: total > 0 ? (current / total) * 100 : 0 }
})

const toolCalls = computed(() => {
    const actions = events.value.filter(e => e.type === AgentEventType.ACTION && e.subType === ActionSubType.TOOL_CALL)
    return actions.map(action => {
        const result = events.value.find(e =>
            e.type === AgentEventType.OBSERVATION &&
            e.subType === ObservationSubType.TOOL_RESULT &&
            e.payload?.data?.toolName === action.payload.name
        )
        return { ...action, result: result?.payload?.data }
    })
})

const ragSources = computed(() => {
    const ev = events.value.find(e => e.type === AgentEventType.OBSERVATION && e.subType === ObservationSubType.RAG)
    return ev?.payload?.data?.sources || []
})
</script>
