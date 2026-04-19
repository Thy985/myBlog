<template>
    <div class="agent-chat h-full flex flex-col bg-gradient-tech">
        <div ref="listRef" class="flex-1 overflow-y-auto p-4 space-y-4" @scroll="onScroll">
            <div v-if="!sessionStore.hasMessages && !executionStore.isExecuting" class="flex flex-col items-center justify-center h-full text-center">
                <div class="w-20 h-20 mb-5 rounded-2xl bg-gradient-to-br from-[var(--agent-primary)] to-[#8B5CF6] flex items-center justify-center shadow-glow-lg">
                    <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path></svg>
                </div>
                <h3 class="text-xl font-bold text-gradient-purple mb-2">AI 智能助手</h3>
                <p class="text-[var(--agent-text-muted)] text-sm max-w-xs mb-6">我可以帮您发布博客、回答技术问题、代码调试等</p>
            </div>
            <template v-else>
                <AgentMessage v-for="msg in sessionStore.messages" :key="msg.id" :message="msg" :execution="getExecution(msg)" @copy="onCopy" @retry="onRetry" />
            </template>
            <div v-if="executionStore.isExecuting && !sessionStore.hasMessages" class="flex justify-start gap-3">
                <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-[var(--agent-primary)] to-[#8B5CF6] flex items-center justify-center shadow-glow flex-shrink-0">
                    <svg class="w-4 h-4 text-white animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                </div>
                <div class="agent-msg-ai px-4 py-3"><div class="agent-loading-dots"><span></span><span></span><span></span></div></div>
            </div>
        </div>
        <AgentInputArea ref="inputRef" :is-executing="executionStore.isExecuting" :current-execution="executionStore.currentExecution" :has-messages="sessionStore.hasMessages" :quick-commands="quickCommands" @send="onSend" @cancel="onCancel" @quick-command="onQuickCommand" />
        <div v-if="sessionStore.hasMessages && !executionStore.isExecuting" class="px-4 pb-2 flex justify-end">
            <button class="text-xs text-[var(--agent-text-muted)] hover:text-[var(--agent-error)] transition-colors flex items-center gap-1" @click="onClear">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                清空会话
            </button>
        </div>
    </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useAgentSessionStore } from '@/stores/agentSession'
import { useAgentExecutionStore } from '@/stores/agentExecution'
import { agentService } from '@/services/agentService'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import AgentMessage from './agent/AgentMessage.vue'
import AgentInputArea from './agent/AgentInputArea.vue'

const sessionStore = useAgentSessionStore()
const executionStore = useAgentExecutionStore()

const listRef = ref(null)
const inputRef = ref(null)
const autoScroll = ref(true)

const quickCommands = [
    { label: '发博客', icon: '📝', command: '发一篇关于{topic}的博客', topic: 'React' },
    { label: '技术问答', icon: '❓', command: '请解释一下{topic}的原理', topic: 'TypeScript泛型' },
    { label: '代码debug', icon: '🐛', command: '帮我看看这段代码有什么问题' },
    { label: '内容摘要', icon: '📋', command: '帮我总结一下这篇文章的主要内容' }
]

function getExecution(msg) {
    if (msg.role !== 'assistant') {return null}
    if (executionStore.currentExecution?.messageId === msg.id) {return executionStore.currentExecution}
    return executionStore.executionHistory.find(e => e.messageId === msg.id) || null
}

async function onSend(content) {
    await agentService.sendMessage(content)
    autoScroll.value = true
    nextTick(() => scrollToBottom(true))
}

function onCancel() { agentService.cancelStream() }

function onQuickCommand(cmd) {
    let text = cmd.command
    if (cmd.topic) {text = text.replace('{topic}', cmd.topic)}
    onSend(text)
}

async function onCopy(content) {
    try { await navigator.clipboard.writeText(content); ElMessage.success('已复制') }
    catch (e) { logger.debug('复制失败:', e); ElMessage.error('复制失败') }
}

async function onRetry(messageId) { await agentService.retryMessage(messageId) }

function onClear() { agentService.clearSession(); ElMessage.success('会话已清空') }

function onScroll() {
    if (!listRef.value) {return}
    const { scrollTop, scrollHeight, clientHeight } = listRef.value
    autoScroll.value = scrollHeight - scrollTop - clientHeight < 100
}

function scrollToBottom(smooth = false) {
    nextTick(() => {
        if (!listRef.value) {return}
        listRef.value.scrollTo({ top: listRef.value.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
    })
}

onMounted(() => { agentService.init() })

onBeforeUnmount(() => {
  agentService.cancelStream()
})
</script>
