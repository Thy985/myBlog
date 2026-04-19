<template>
  <!-- 移动端导航菜单 -->
  <Transition
    name="slide-down"
    @enter="onEnter"
    @after-enter="onAfterEnter"
    @leave="onLeave"
  >
    <div
      v-if="isOpen"
      id="navbar-search"
      class="md:hidden py-4 border-t border-border-color mobile-menu-container"
    >
      <!-- 移动端搜索框 - 带搜索建议 -->
      <div class="relative mb-4">
        <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
          <svg class="w-4 h-4 text-text-tertiary" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z" />
          </svg>
        </div>
        <input
          v-model="searchKeyword"
          type="text"
          class="w-full px-4 py-2.5 pl-10 text-sm border border-border-color rounded-lg bg-background-secondary focus:border-primary-color focus:ring-2 focus:ring-primary-color/20 outline-none transition-all duration-200"
          placeholder="搜索文章..."
          @keyup.enter="handleSearch"
          @focus="showSearchHistory = true"
          @blur="hideSearchHistory"
        >

        <!-- 搜索历史和热门搜索 -->
        <Transition name="fade">
          <div
            v-if="showSearchHistory && (searchHistory.length > 0 || hotSearches.length > 0)"
            class="absolute top-full left-0 right-0 mt-1 glass rounded-lg shadow-lg z-50"
          >
            <!-- 搜索历史 -->
            <div v-if="searchHistory.length > 0" class="p-3">
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs text-gray-500 dark:text-gray-400">搜索历史</span>
                <button class="text-xs text-blue-500 hover:text-blue-600" @click="clearSearchHistory">
                  清空
                </button>
              </div>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="item in searchHistory"
                  :key="item"
                  class="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 rounded cursor-pointer hover:bg-gray-200 dark:hover:bg-gray-600"
                  @click="quickSearch(item)"
                >
                  {{ item }}
                </span>
              </div>
            </div>

            <!-- 热门搜索 -->
            <div v-if="hotSearches.length > 0" class="p-3 border-t border-gray-100 dark:border-gray-700">
              <span class="text-xs text-gray-500 dark:text-gray-400 mb-2 block">热门搜索</span>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="(item, index) in hotSearches"
                  :key="item"
                  class="px-2 py-1 text-xs rounded cursor-pointer hover:opacity-80"
                  :class="getHotSearchClass(index)"
                  @click="quickSearch(item)"
                >
                  {{ item }}
                </span>
              </div>
            </div>
          </div>
        </Transition>
      </div>

      <!-- 移动端导航链接 -->
      <ul class="space-y-1" role="menu">
        <li
          v-for="(item, index) in menuItems"
          :key="item.path"
          role="menuitem"
          :style="{ animationDelay: `${index * 50}ms` }"
          class="menu-item-animate"
        >
          <router-link
            :to="item.path"
            class="block py-3 px-4 rounded-lg hover:bg-primary-subtle transition-all duration-200 touch-manipulation"
            active-class="bg-primary-subtle text-primary-color font-medium"
            :aria-current="$route.path === item.path ? 'page' : undefined"
            @click="handleLinkClick"
          >
            <div class="flex items-center gap-3">
              <component :is="item.icon" class="w-5 h-5" />
              <span>{{ item.name }}</span>
            </div>
          </router-link>
        </li>
      </ul>

      <!-- 用户操作区 -->
      <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
        <div v-if="isLoggedIn" class="space-y-1">
          <router-link
            to="/user"
            class="block py-3 px-4 rounded-lg hover:bg-primary-subtle transition-all duration-200 touch-manipulation"
            @click="handleLinkClick"
          >
            <div class="flex items-center gap-3">
              <UserIcon class="w-5 h-5" />
              <span>个人中心</span>
            </div>
          </router-link>
          <button
            class="w-full text-left py-3 px-4 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 text-red-600 dark:text-red-400 transition-all duration-200 touch-manipulation"
            @click="handleLogout"
          >
            <div class="flex items-center gap-3">
              <ArrowRightOnRectangleIcon class="w-5 h-5" />
              <span>退出登录</span>
            </div>
          </button>
        </div>
        <div v-else class="space-y-1">
          <router-link
            to="/login"
            class="block py-3 px-4 rounded-lg hover:bg-primary-subtle transition-all duration-200 touch-manipulation"
            @click="handleLinkClick"
          >
            <div class="flex items-center gap-3">
              <ArrowRightOnRectangleIcon class="w-5 h-5" />
              <span>登录</span>
            </div>
          </router-link>
          <router-link
            to="/register"
            class="block py-3 px-4 rounded-lg hover:bg-primary-subtle transition-all duration-200 touch-manipulation"
            @click="handleLinkClick"
          >
            <div class="flex items-center gap-3">
              <UserPlusIcon class="w-5 h-5" />
              <span>注册</span>
            </div>
          </router-link>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showMessage, showModel } from '@/utils'
import { useAuthStore } from '@/stores'
import logger from '@/utils/logger'
import {
  HomeIcon,
  FolderIcon,
  TagIcon,
  CalendarIcon,
  UserIcon,
  ArrowRightOnRectangleIcon,
  UserPlusIcon
} from '@heroicons/vue/24/outline'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  isOpen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'close'])

const router = useRouter()
const authStore = useAuthStore()
const searchKeyword = ref(props.modelValue)
const showSearchHistory = ref(false)

// 搜索历史
const searchHistory = ref([])
const SEARCH_HISTORY_KEY = 'mobile_search_history'
const MAX_HISTORY_ITEMS = 8

// 热门搜索（可以从后端获取）
const hotSearches = ref(['Vue3', 'JavaScript', 'TypeScript', 'React', 'Node.js'])

// 检查登录状态
const isLoggedIn = computed(() => authStore.isLoggedIn())

// 加载搜索历史
onMounted(() => {
  const history = localStorage.getItem(SEARCH_HISTORY_KEY)
  if (history) {
    try {
      searchHistory.value = JSON.parse(history)
    } catch (e) {
      logger.error('Failed to parse search history:', e)
    }
  }
})

// 保存搜索历史
const saveSearchHistory = (keyword) => {
  if (!keyword.trim()) {return}

  // 移除重复项
  const index = searchHistory.value.indexOf(keyword)
  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }

  // 添加到开头
  searchHistory.value.unshift(keyword)

  // 限制数量
  if (searchHistory.value.length > MAX_HISTORY_ITEMS) {
    searchHistory.value = searchHistory.value.slice(0, MAX_HISTORY_ITEMS)
  }

  // 保存到本地存储
  localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(searchHistory.value))
}

// 清空搜索历史
const clearSearchHistory = () => {
  searchHistory.value = []
  localStorage.removeItem(SEARCH_HISTORY_KEY)
}

// 快速搜索
const quickSearch = (keyword) => {
  searchKeyword.value = keyword
  handleSearch()
}

// 隐藏搜索历史（延迟以允许点击）
const hideSearchHistory = () => {
  setTimeout(() => {
    showSearchHistory.value = false
  }, 200)
}

// 热门搜索样式
const getHotSearchClass = (index) => {
  const classes = [
    'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
    'bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400',
    'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
    'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
    'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
  ]
  return classes[index % classes.length]
}

// 使用 Heroicons 图标组件
const menuItems = [
  { name: '首页', path: '/', icon: HomeIcon },
  { name: '分类', path: '/category', icon: FolderIcon },
  { name: '标签', path: '/tag', icon: TagIcon },
  { name: '归档', path: '/archive', icon: CalendarIcon }
]

const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    showMessage('搜索关键词不能为空', 'warning')
    return
  }

  // 保存搜索历史
  saveSearchHistory(searchKeyword.value.trim())

  emit('update:modelValue', searchKeyword.value)
  emit('search', searchKeyword.value.trim())

  router.push({
    path: '/search',
    query: { keyword: searchKeyword.value.trim() }
  })

  // 关闭菜单
  emit('close')
}

const handleLinkClick = () => {
  emit('close')
}

const handleLogout = async () => {
  try {
    await showModel('确定要退出登录吗？', 'warning', '确认退出')
    authStore.logout()
    showMessage('退出登录成功', 'success')
    emit('close')
    router.push('/')
  } catch (error) {
    // 用户取消
  }
}

// 动画回调
const onEnter = (el) => {
  el.style.opacity = '0'
  el.style.transform = 'translateY(-10px)'
}

const onAfterEnter = (el) => {
  el.style.opacity = '1'
  el.style.transform = 'translateY(0)'
}

const onLeave = (el) => {
  el.style.opacity = '0'
  el.style.transform = 'translateY(-10px)'
}

// 监听 props 变化
watch(() => props.modelValue, (newVal) => {
  searchKeyword.value = newVal
})

watch(() => props.isOpen, (newVal) => {
  if (!newVal) {
    showSearchHistory.value = false
  }
})
</script>

<style scoped>
/* 触摸优化 */
.touch-manipulation {
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
}

/* 玻璃拟态菜单容器 */
.mobile-menu-container {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur-lg));
  -webkit-backdrop-filter: blur(var(--glass-blur-lg));
  border-top: 1px solid var(--glass-border);
}

.dark .mobile-menu-container {
  background: rgba(10, 10, 11, 0.95);
}

/* 菜单动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 菜单项动画 */
.menu-item-animate {
  opacity: 0;
  animation: menuItemSlide 0.3s ease forwards;
}

@keyframes menuItemSlide {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 减少动画模式支持 */
@media (prefers-reduced-motion: reduce) {
  .menu-item-animate {
    animation: none;
    opacity: 1;
  }

  .slide-down-enter-active,
  .slide-down-leave-active {
    transition: none;
  }

  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}
</style>
