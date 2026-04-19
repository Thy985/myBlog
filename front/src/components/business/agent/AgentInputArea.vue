<template>
    <div class="agent-input-wrap">
        <div v-if="isExecuting" class="mb-3">
            <AgentThoughtProcess v-if="currentExecution" :execution="currentExecution" />
        </div>

        <div v-if="quickCommands && !isExecuting && !hasMessages" class="flex flex-wrap gap-2 mb-3">
            <button
                v-for="cmd in quickCommands"
                :key="cmd.label"
                class="agent-quick-cmd"
                @click="$emit('quick-command', cmd)"
            >
                <span>{{ cmd.icon }}</span>
                <span>{{ cmd.label }}</span>
            </button>
        </div>

        <div class="flex gap-2 items-end">
            <textarea
                ref="textareaRef"
                v-model="text"
                :disabled="isExecuting"
                placeholder="输入消息..."
                maxlength="2000"
                rows="1"
                class="agent-input"
                @keydown.enter.prevent="onEnter"
                @keydown.escape="$emit('cancel')"
                @input="autoResize"
            ></textarea>
            <button v-if="isExecuting" class="agent-cancel-btn" title="取消" @click="$emit('cancel')">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
            <button v-else class="agent-send-btn" :disabled="!canSend" title="发送" @click="onSend">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path>
                </svg>
            </button>
        </div>

        <div class="flex items-center justify-between mt-2">
            <span class="text-[10px] text-[var(--agent-text-muted)]" :class="{ 'text-[var(--agent-error)]': text.length > 1800 }">
                {{ text.length }}/2000
            </span>
            <div class="flex items-center gap-4 text-[10px] text-[var(--agent-text-muted)]">
                <span><kbd class="px-1 py-0.5 bg-[var(--agent-bg-card)] rounded text-[10px]">Enter</kbd> 发送</span>
                <span><kbd class="px-1 py-0.5 bg-[var(--agent-bg-card)] rounded text-[10px]">Shift+Enter</kbd> 换行</span>
                <span><kbd class="px-1 py-0.5 bg-[var(--agent-bg-card)] rounded text-[10px]">Esc</kbd> 取消</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import AgentThoughtProcess from './AgentThoughtProcess.vue'

const props = defineProps({
    isExecuting: { type: Boolean, default: false },
    currentExecution: { type: Object, default: null },
    hasMessages: { type: Boolean, default: false },
    quickCommands: { type: Array, default: () => [] }
})

const emit = defineEmits(['send', 'cancel', 'quick-command'])

const text = ref('')
const textareaRef = ref(null)

const canSend = computed(() => text.value.trim().length > 0 && text.value.length <= 2000)

function onEnter(e) {
    if (e.shiftKey) {
        text.value += '\n'
        autoResize()
    } else {
        onSend()
    }
}

function onSend() {
    if (!canSend.value || props.isExecuting) {return}
    emit('send', text.value.trim())
    text.value = ''
    nextTick(() => autoResize())
}

function autoResize() {
    nextTick(() => {
        const el = textareaRef.value
        if (!el) {return}
        el.style.height = 'auto'
        el.style.height = Math.min(Math.max(el.scrollHeight, 44), 120) + 'px'
    })
}

function focus() {
    textareaRef.value?.focus()
}

defineExpose({ focus, text })
</script>
