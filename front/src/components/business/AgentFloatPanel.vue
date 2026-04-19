<template>
    <div class="agent-float-panel">
        <!-- 悬浮球（收起状态）-->
        <Transition name="scale">
            <button
                v-if="!isExpanded"
                class="fixed z-50 rounded-full bg-gradient-to-br from-primary to-success shadow-glow-lg flex items-center justify-center text-white transition-transform hover:scale-110 active:scale-95"
                :style="ballStyle"
                :class="{ 'scale-90': isNearEdge }"
                aria-label="打开 AI 助手"
                @click="handleToggle"
                @pointerdown="startDrag"
            >
                <!-- 未读消息指示 -->
                <span
                    v-if="uiStore.unreadCount > 0"
                    class="absolute -top-1 -right-1 min-w-[1.25rem] h-5 bg-tech-error rounded-full text-[10px] font-bold flex items-center justify-center px-1 shadow-glow-sm animate-in fade-in slide-in-from-top-1"
                >
                    {{ uiStore.unreadCount > 9 ? '9+' : uiStore.unreadCount }}
                </span>
                <svg class="w-7 h-7 relative z-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
                </svg>
            </button>
        </Transition>

        <!-- 展开面板 -->
        <Transition name="slide-up">
            <div
                v-if="isExpanded"
                class="fixed z-50 rounded-2xl glass-strong shadow-2xl border border-white/10 flex flex-col overflow-hidden"
                :style="panelStyle"
                role="dialog"
                aria-label="AI 智能助手"
            >
                <!-- 拖动标题栏 -->
                <div
                    class="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-primary to-success text-white cursor-move select-none touch-none"
                    @pointerdown="startPanelDrag"
                >
                    <div class="flex items-center gap-2">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
                        </svg>
                        <span class="font-medium">AI 智能助手</span>
                    </div>
                    <div class="flex items-center gap-1">
                        <!-- 最小化按钮 -->
                        <button
                            class="p-1.5 hover:bg-white/20 rounded-lg transition-colors"
                            aria-label="最小化"
                            @click="toggleExpand"
                        >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"></path>
                            </svg>
                        </button>
                        <!-- 关闭按钮 -->
                        <button
                            class="p-1.5 hover:bg-white/20 rounded-lg transition-colors"
                            aria-label="关闭"
                            @click="handleClose"
                        >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                            </svg>
                        </button>
                    </div>
                </div>

                <!-- 内容区 -->
                <div class="flex-1 overflow-hidden">
                    <AgentChat />
                </div>
            </div>
        </Transition>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAgentUIStore } from '@/stores/agentUI'
import AgentChat from './AgentChat.vue'
import logger from '@/utils/logger'

const uiStore = useAgentUIStore()

// ============ 配置常量 ============
const CONFIG = {
    BALL_SIZE: 56,
    EDGE_SNAP_THRESHOLD: 60,
    DEFAULT_PANEL_WIDTH: 400,
    DEFAULT_PANEL_HEIGHT: 520,
    MIN_PANEL_SIZE: 200,
    STORAGE_KEY: 'blog:agent:position:v1'
}

// ============ 状态 ============
const isExpanded = ref(false)
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0, posX: 0, posY: 0 })

// 位置状态（使用 right/bottom 定位）
const position = ref({ x: 24, y: 100 })
const panelSize = ref({
    width: CONFIG.DEFAULT_PANEL_WIDTH,
    height: CONFIG.DEFAULT_PANEL_HEIGHT
})

// ============ 计算属性 ============
const isNearEdge = computed(() => {
    return position.value.x < CONFIG.EDGE_SNAP_THRESHOLD ||
           position.value.x > window.innerWidth - CONFIG.EDGE_SNAP_THRESHOLD - CONFIG.BALL_SIZE
})

const ballStyle = computed(() => ({
    right: `${position.value.x}px`,
    bottom: `${position.value.y}px`,
    width: `${CONFIG.BALL_SIZE}px`,
    height: `${CONFIG.BALL_SIZE}px`
}))

const panelStyle = computed(() => {
    const isMobile = window.innerWidth < 640
    const width = isMobile ? Math.min(window.innerWidth - 16, 400) : panelSize.value.width
    const height = isMobile ? Math.min(window.innerHeight - 100, 600) : panelSize.value.height

    return {
        right: `${position.value.x}px`,
        bottom: `${position.value.y}px`,
        width: `${width}px`,
        height: `${height}px`
    }
})

// ============ 拖动逻辑 ============
function startDrag(e) {
    if (isExpanded.value) {return}

    isDragging.value = true
    dragStart.value = {
        x: e.clientX,
        y: e.clientY,
        posX: position.value.x,
        posY: position.value.y
    }

    // 使用 pointer 事件统一处理鼠标和触摸
    e.target.setPointerCapture(e.pointerId)
}

function startPanelDrag(e) {
    isDragging.value = true
    dragStart.value = {
        x: e.clientX,
        y: e.clientY,
        posX: position.value.x,
        posY: position.value.y
    }

    e.target.setPointerCapture(e.pointerId)
}

function onDrag(e) {
    if (!isDragging.value) {return}

    const deltaX = dragStart.value.x - e.clientX
    const deltaY = dragStart.value.y - e.clientY

    let newX = dragStart.value.posX + deltaX
    let newY = dragStart.value.posY + deltaY

    // 边界限制
    newX = Math.max(0, Math.min(window.innerWidth - CONFIG.BALL_SIZE, newX))
    newY = Math.max(0, Math.min(window.innerHeight - CONFIG.BALL_SIZE, newY))

    position.value.x = newX
    position.value.y = newY
}

function stopDrag(e) {
    if (!isDragging.value) {return}

    isDragging.value = false
    e.target.releasePointerCapture(e.pointerId)

    // 计算是否是真正的拖动（还是只是点击）
    const deltaX = Math.abs(e.clientX - dragStart.value.x)
    const deltaY = Math.abs(e.clientY - dragStart.value.y)
    const wasDrag = deltaX > 5 || deltaY > 5

    if (wasDrag) {
        // 吸附到边缘
        snapToEdge()
        savePosition()
    }
}

// ============ 边缘吸附 ============
function snapToEdge() {
    const { x, y } = position.value
    const threshold = CONFIG.EDGE_SNAP_THRESHOLD
    const windowWidth = window.innerWidth
    const windowHeight = window.innerHeight

    let newX = x
    let newY = y

    // 水平吸附（左右边缘）
    if (x < threshold) {
        newX = 8
    } else if (x > windowWidth - threshold - CONFIG.BALL_SIZE) {
        newX = windowWidth - CONFIG.BALL_SIZE - 8
    }

    // 垂直吸附（底部）
    if (y < threshold) {
        newY = 8
    } else if (y > windowHeight - threshold - CONFIG.BALL_SIZE) {
        newY = windowHeight - CONFIG.BALL_SIZE - 8
    }

    position.value.x = newX
    position.value.y = newY
}

// ============ 面板操作 ============
function toggleExpand() {
    isExpanded.value = !isExpanded.value
    if (isExpanded.value) {
        adjustPanelPosition()
        uiStore.clearUnread()
    }
}

function handleToggle() {
    // 如果是拖动操作，不触发点击
    if (isDragging.value) {return}
    toggleExpand()
}

function handleClose() {
    isExpanded.value = false
}

function adjustPanelPosition() {
    const maxX = window.innerWidth - panelSize.value.width - 8
    const maxY = window.innerHeight - panelSize.value.height - 8

    position.value.x = Math.max(8, Math.min(position.value.x, maxX))
    position.value.y = Math.max(8, Math.min(position.value.y, maxY))
}

// ============ 存储操作 ============
function savePosition() {
    try {
        localStorage.setItem(CONFIG.STORAGE_KEY, JSON.stringify({
            x: position.value.x,
            y: position.value.y,
            savedAt: Date.now()
        }))
    } catch (e) {
        logger.warn('[AgentFloatPanel] Failed to save position:', e)
    }
}

function loadPosition() {
    try {
        const saved = localStorage.getItem(CONFIG.STORAGE_KEY)
        if (saved) {
            const pos = JSON.parse(saved)
            position.value.x = Math.max(8, pos.x ?? 24)
            position.value.y = Math.max(8, pos.y ?? 100)

            // 确保位置在屏幕内
            snapToEdge()
        }
    } catch (e) {
        logger.warn('[AgentFloatPanel] Failed to load position:', e)
    }
}

// ============ 事件处理 ============
function handleKeydown(e) {
    // Escape 关闭面板
    if (e.key === 'Escape' && isExpanded.value) {
        handleClose()
    }
    // Ctrl/Cmd + K 打开/关闭面板
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault()
        toggleExpand()
    }
}

function handleResize() {
    snapToEdge()
    adjustPanelPosition()
}

// ============ 生命周期 ============
onMounted(() => {
    loadPosition()

    // 全局事件监听
    document.addEventListener('pointermove', onDrag)
    document.addEventListener('pointerup', stopDrag)
    document.addEventListener('pointercancel', stopDrag)
    document.addEventListener('keydown', handleKeydown)
    window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
    // 清理事件监听
    document.removeEventListener('pointermove', onDrag)
    document.removeEventListener('pointerup', stopDrag)
    document.removeEventListener('pointercancel', stopDrag)
    document.removeEventListener('keydown', handleKeydown)
    window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 过渡动画 */
.scale-enter-active,
.scale-leave-active {
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.scale-enter-from,
.scale-leave-to {
    opacity: 0;
    transform: scale(0.8);
}

.slide-up-enter-active,
.slide-up-leave-active {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
}

/* 悬停效果 */
button:hover {
    box-shadow: 0 0 20px rgba(99, 102, 241, 0.4), 0 8px 16px rgba(0, 0, 0, 0.2);
}

/* 减少动画 - 尊重用户偏好 */
@media (prefers-reduced-motion: reduce) {
    *,
    *::before,
    *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
    }
}

/* 移动端适配 */
@media (max-width: 640px) {
    .agent-float-panel button {
        width: 3rem !important;
        height: 3rem !important;
    }
}
</style>
