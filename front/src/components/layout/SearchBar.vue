<template>
  <!-- 搜索框（桌面端） -->
  <div ref="searchContainer" class="relative w-48 lg:w-64">
    <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
      <svg class="w-4 h-4 text-text-muted" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z" />
      </svg>
    </div>
    <input
      id="search-input-desktop"
      v-model="keyword"
      type="text"
      class="search-input"
      :class="{ 'rounded-b-none': showDropdown }"
      placeholder="搜索..."
      aria-label="搜索文章"
      autocomplete="off"
      @focus="handleFocus"
      @blur="handleBlur"
      @keyup.enter="handleSearch"
      @keyup.up.prevent="navigateSuggestion(-1)"
      @keyup.down.prevent="navigateSuggestion(1)"
    >
    <button
      class="absolute inset-y-0 right-0 flex items-center pr-3 text-text-muted hover:text-primary-color transition-colors duration-200"
      aria-label="执行搜索"
      @click="handleSearch"
    >
      <svg class="w-4 h-4" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z" />
      </svg>
    </button>

    <!-- 搜索下拉框 -->
    <div
      v-if="showDropdown"
      class="absolute z-50 w-full bg-background-primary border border-border-color rounded-lg rounded-t-none shadow-lg overflow-hidden"
    >
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="py-2">
        <div class="px-3 py-1 text-xs text-text-muted uppercase tracking-wide">建议</div>
        <div
          v-for="(suggestion, index) in suggestions"
          :key="index"
          class="px-4 py-2 cursor-pointer hover:bg-background-secondary transition-colors"
          :class="{ 'bg-background-secondary': selectedIndex === index }"
          @mousedown.prevent="selectSuggestion(suggestion)"
        >
          <span class="text-text-primary">{{ suggestion }}</span>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0 && keyword === ''" class="py-2">
        <div class="px-3 py-1 flex items-center justify-between">
          <span class="text-xs text-text-muted uppercase tracking-wide">搜索历史</span>
          <button class="text-xs text-primary-color hover:underline" @click="clearHistory">清除</button>
        </div>
        <div
          v-for="(history, index) in searchHistory"
          :key="index"
          class="px-4 py-2 cursor-pointer hover:bg-background-secondary transition-colors flex items-center gap-2"
          :class="{ 'bg-background-secondary': selectedIndex === suggestions.length + index }"
          @mousedown.prevent="selectHistory(history)"
        >
          <svg class="w-3 h-3 text-text-muted" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span class="text-text-primary">{{ history }}</span>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div v-if="keyword === ''" class="py-2 border-t border-border-color">
        <div class="px-3 py-1 text-xs text-text-muted uppercase tracking-wide">热门搜索</div>
        <div
          v-for="(hot, index) in hotSearches"
          :key="index"
          class="px-4 py-2 cursor-pointer hover:bg-background-secondary transition-colors flex items-center gap-2"
          @mousedown.prevent="selectHistory(hot)"
        >
          <span
            class="w-5 h-5 rounded text-xs flex items-center justify-center"
            :class="index < 3 ? 'bg-primary-color text-white' : 'bg-background-secondary text-text-muted'"
          >
            {{ index + 1 }}
          </span>
          <span class="text-text-primary">{{ hot }}</span>
        </div>
      </div>

      <!-- 无结果提示 -->
      <div v-if="suggestions.length === 0 && keyword !== ''" class="py-6 text-center text-text-muted">
        未找到相关搜索建议
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showMessage } from '@/utils'
import logger from '@/utils/logger'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'search'])

const router = useRouter()
const keyword = ref(props.modelValue)
const showDropdown = ref(false)
const suggestions = ref([])
const selectedIndex = ref(-1)
const searchHistory = ref([])
const hotSearches = ref(['Vue3 教程', 'React 入门', 'TypeScript 实践', 'Tailwind CSS'])

// 模拟搜索建议
const mockSuggestions = [
  'Vue3 Composition API 详解',
  'Vue3 Teleport 组件原理',
  'Vue3 响应式原理分析',
  'React 18 新特性',
  'React Hooks 深入理解',
  'TypeScript 类型体操',
  'JavaScript 设计模式',
  '前端性能优化技巧'
]

// 监听输入（带防抖）
let debounceTimer = null

watch(keyword, (newVal) => {
  if (debounceTimer) {clearTimeout(debounceTimer)}

  debounceTimer = setTimeout(() => {
    if (newVal.trim()) {
      suggestions.value = mockSuggestions
        .filter(s => s.toLowerCase().includes(newVal.toLowerCase()))
        .slice(0, 5)
      selectedIndex.value = -1
    } else {
      suggestions.value = []
    }
  }, 300)
})

// 处理焦点
const handleFocus = () => {
  showDropdown.value = true
  loadHistory()
}

// 处理失焦
const handleBlur = () => {
  // 延迟关闭，以便点击事件可以触发
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

// 加载搜索历史
const loadHistory = () => {
  try {
    const history = localStorage.getItem('searchHistory')
    if (history) {
      searchHistory.value = JSON.parse(history).slice(0, 5)
    }
  } catch (e) {
    searchHistory.value = []
  }
}

// 保存搜索历史
const saveHistory = (keyword) => {
  try {
    let history = []
    const stored = localStorage.getItem('searchHistory')
    if (stored) {
      history = JSON.parse(stored)
    }
    // 移除已存在的相同项
    history = history.filter(h => h !== keyword)
    // 添加到开头
    history.unshift(keyword)
    // 只保留10条
    history = history.slice(0, 10)
    localStorage.setItem('searchHistory', JSON.stringify(history))
  } catch (e) {
    logger.error('保存搜索历史失败:', e)
  }
}

// 清除历史
const clearHistory = () => {
  try {
    localStorage.removeItem('searchHistory')
    searchHistory.value = []
  } catch (e) {
    logger.error('清除搜索历史失败:', e)
  }
}

// 选择建议
const selectSuggestion = (suggestion) => {
  keyword.value = suggestion
  suggestions.value = []
  showDropdown.value = false
  performSearch()
}

// 选择历史
const selectHistory = (history) => {
  keyword.value = history
  showDropdown.value = false
  performSearch()
}

// 导航建议列表
const navigateSuggestion = (direction) => {
  const totalItems = suggestions.value.length + searchHistory.value.length
  if (totalItems === 0) {return}

  selectedIndex.value += direction
  if (selectedIndex.value < -1) {
    selectedIndex.value = totalItems - 1
  } else if (selectedIndex.value >= totalItems) {
    selectedIndex.value = -1
  }
}

// 执行搜索
const performSearch = () => {
  if (!keyword.value.trim()) {
    showMessage('搜索关键词不能为空', 'warning')
    return
  }

  saveHistory(keyword.value.trim())
  emit('update:modelValue', keyword.value)
  emit('search', keyword.value.trim())

  router.push({
    path: '/search',
    query: { keyword: keyword.value.trim() }
  })
}

// 暴露搜索方法
const handleSearch = () => {
  if (selectedIndex.value >= 0 && selectedIndex.value < suggestions.value.length) {
    selectSuggestion(suggestions.value[selectedIndex.value])
  } else {
    performSearch()
  }
}

// 点击外部关闭
const handleClickOutside = (event) => {
  if (searchContainer.value && !searchContainer.value.contains(event.target)) {
    showDropdown.value = false
  }
}

const searchContainer = ref(null)

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (debounceTimer) {clearTimeout(debounceTimer)}
})
</script>

<style scoped>
/* 搜索框玻璃拟态样式 */
.search-input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  font-size: 14px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 8px;
  color: var(--text-primary);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  transition: all 0.2s ease;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.search-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-subtle);
}

/* 下拉框暗色模式 */
.absolute.z-50 {
  background-color: var(--bg-card);
  border: 1px solid var(--border-color);
  backdrop-filter: blur(var(--glass-blur));
}
</style>
