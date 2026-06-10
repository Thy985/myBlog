<template>
    <nav v-if="total > 0" aria-label="分页导航" class="pagination-container">
        <!-- 分页按钮组 -->
        <ul class="flex items-center justify-center space-x-1 text-sm">
            <li>
                <a
                    v-if="current > 1"
                    class="page-btn"
                    aria-label="上一页"
                    tabindex="0"
                    @click="handlePageChange(current - 1)"
                    @keydown.enter="handlePageChange(current - 1)"
                    @keydown.space.prevent="handlePageChange(current - 1)"
                >
                    <svg class="w-4 h-4" aria-hidden="true" fill="none" viewBox="0 0 6 10">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 1 1 5l4 4" />
                    </svg>
                </a>
                <a
                    v-else
                    class="page-btn page-btn-disabled"
                    aria-label="上一页"
                    tabindex="0"
                >
                    <svg class="w-4 h-4" aria-hidden="true" fill="none" viewBox="0 0 6 10">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 1 1 5l4 4" />
                    </svg>
                </a>
            </li>
            <li v-for="page in displayPages" :key="page">
                <a
                    v-if="page === '...'"
                    class="page-ellipsis"
                >
                    ...
                </a>
                <a
                    v-else
                    class="page-btn"
                    :class="{ 'page-btn-active': page === current }"
                    :aria-current="page === current ? 'page' : undefined"
                    :aria-label="`第 ${page} 页`"
                    tabindex="0"
                    @click="handlePageChange(page)"
                    @keydown.enter="handlePageChange(page)"
                    @keydown.space.prevent="handlePageChange(page)"
                >
                    {{ page }}
                </a>
            </li>
            <li>
                <a
                    v-if="current < pages"
                    class="page-btn"
                    aria-label="下一页"
                    tabindex="0"
                    @click="handlePageChange(current + 1)"
                    @keydown.enter="handlePageChange(current + 1)"
                    @keydown.space.prevent="handlePageChange(current + 1)"
                >
                    <svg class="w-4 h-4" aria-hidden="true" fill="none" viewBox="0 0 6 10">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 9 4-4-4-4" />
                    </svg>
                </a>
                <a
                    v-else
                    class="page-btn page-btn-disabled"
                    aria-label="下一页"
                    tabindex="0"
                >
                    <svg class="w-4 h-4" aria-hidden="true" fill="none" viewBox="0 0 6 10">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 9 4-4-4-4" />
                    </svg>
                </a>
            </li>
        </ul>

        <!-- 分页信息栏 -->
        <div class="pagination-info">
            <!-- 总记录数 -->
            <span class="text-text-muted text-sm">
                共 <strong class="text-text-primary">{{ total }}</strong> 条
            </span>

            <!-- 快速跳转 -->
            <div class="jump-to-page">
                <label for="jumpInput" class="text-text-muted text-sm">跳转到</label>
                <input
                    id="jumpInput"
                    v-model.number="jumpPage"
                    type="number"
                    :min="1"
                    :max="pages"
                    class="jump-input"
                    @keyup.enter="handleJumpToPage"
                />
                <button
                    class="jump-btn"
                    :disabled="!isValidJumpPage"
                    @click="handleJumpToPage"
                >
                    跳转
                </button>
            </div>

            <!-- 每页条数选择器 -->
            <div class="page-size-selector">
                <label for="pageSizeSelect" class="text-text-muted text-sm">每页</label>
                <select
                    id="pageSizeSelect"
                    v-model.number="localSize"
                    class="size-select"
                    @change="handleSizeChange"
                >
                    <option v-for="s in sizeOptions" :key="s" :value="s">{{ s }} 条/页</option>
                </select>
            </div>
        </div>
    </nav>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  current?: number
  total?: number
  size?: number
  pages?: number
}>()

const emit = defineEmits<{
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
}>()

const jumpPage = ref(props.current)
const localSize = ref(props.size)
const sizeOptions = [10, 20, 50, 100]

// 计算显示的页码（带省略号）
const displayPages = computed(() => {
    const total = props.pages
    const current = props.current

    if (total <= 7) {
        return Array.from({ length: total }, (_, i) => i + 1)
    }

    const pages = []
    pages.push(1)

    if (current > 3) {
        pages.push('...')
    }

    const start = Math.max(2, current - 1)
    const end = Math.min(total - 1, current + 1)

    for (let i = start; i <= end; i++) {
        pages.push(i)
    }

    if (current < total - 2) {
        pages.push('...')
    }

    pages.push(total)

    return pages
})

// 验证跳转页码是否有效
const isValidJumpPage = computed(() => {
    return jumpPage.value >= 1 && jumpPage.value <= props.pages && Number.isInteger(jumpPage.value)
})

// 处理页码变化
const handlePageChange = (page) => {
    if (page >= 1 && page <= props.pages) {
        emit('page-change', page)
        jumpPage.value = page
    }
}

// 处理快速跳转
const handleJumpToPage = () => {
    if (isValidJumpPage.value && jumpPage.value !== props.current) {
        emit('page-change', jumpPage.value)
    } else {
        // 重置为当前页
        jumpPage.value = props.current
    }
}

// 处理每页条数变化
const handleSizeChange = () => {
    emit('size-change', localSize.value)
}

// 监听外部props变化
watch(() => props.current, (newVal) => {
    jumpPage.value = newVal
})

watch(() => props.size, (newVal) => {
    localSize.value = newVal
})
</script>

<style scoped>
.pagination-container {
    margin-top: 2rem;
    margin-bottom: 2rem;
}

.page-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 2.5rem;
    height: 2.5rem;
    border-radius: var(--radius-md);
    border: 1px solid var(--border-color);
    background: var(--bg-primary);
    color: var(--text-secondary);
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.page-btn:hover:not(.page-btn-disabled):not(.page-btn-active) {
    background: var(--color-primary-subtle);
    border-color: var(--color-primary);
    color: var(--color-primary);
}

.page-btn:focus-visible {
    outline: none;
    box-shadow: var(--focus-outline);
}

.page-btn-active {
    background: var(--color-primary);
    border-color: var(--color-primary);
    color: white;
}

.page-btn-active:hover {
    background: var(--color-primary-hover);
}

.page-btn-disabled {
    opacity: 0.5;
    cursor: not-allowed;
    color: var(--text-muted);
}

.page-ellipsis {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 2.5rem;
    height: 2.5rem;
    color: var(--text-muted);
    user-select: none;
}

.pagination-info {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 1.5rem;
    margin-top: 1rem;
    padding-top: 1rem;
    border-top: 1px solid var(--border-subtle);
    flex-wrap: wrap;
}

.jump-to-page,
.page-size-selector {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.jump-input {
    width: 3.5rem;
    height: 2rem;
    padding: 0 0.5rem;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    background: var(--bg-primary);
    color: var(--text-primary);
    font-size: 0.875rem;
    text-align: center;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.jump-input:focus {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: var(--focus-outline);
}

.jump-btn {
    padding: 0.25rem 0.75rem;
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: white;
    font-size: 0.875rem;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.jump-btn:hover:not(:disabled) {
    background: var(--color-primary-hover);
}

.jump-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.size-select {
    height: 2rem;
    padding: 0 0.5rem;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    background: var(--bg-primary);
    color: var(--text-primary);
    font-size: 0.875rem;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.size-select:focus {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: var(--focus-outline);
}

@media (max-width: 768px) {
    .pagination-info {
        flex-direction: column;
        gap: 0.75rem;
    }
}
</style>
