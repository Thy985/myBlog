<template>
  <!-- 移动端目录导航按钮 -->
  <div class="lg:hidden mb-4">
    <button
      class="flex items-center justify-between w-full p-3 bg-gray-100 dark:bg-gray-700 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
      @click="$emit('toggle')"
    >
      <span class="font-medium">文章目录</span>
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
      </svg>
    </button>
  </div>

  <!-- 移动端目录导航弹窗 -->
  <div
    v-if="visible"
    class="lg:hidden fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4"
  >
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md max-h-[80vh] overflow-y-auto">
      <div class="p-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">文章目录</h3>
        <button
          class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
          @click="$emit('close')"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>
      <div class="p-4">
        <ul class="space-y-2">
          <li v-for="(item, index) in tocItems" :key="item.id || `toc-${index}`">
            <a
              :href="'#' + item.id"
              :class="[
                'block py-2 px-3 rounded transition-colors',
                activeIndex === index
                  ? 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300'
                  : 'hover:bg-gray-100 dark:hover:bg-gray-700'
              ]"
              :style="{ paddingLeft: (item.level - 2) * 16 + 'px' }"
              @click.prevent="$emit('select', item.id, index)"
            >
              {{ item.title }}
            </a>
          </li>
        </ul>
        <div v-if="tocItems.length === 0" class="text-gray-500 text-sm dark:text-gray-400 py-4">
          暂无目录
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  tocItems: {
    type: Array,
    default: () => []
  },
  visible: {
    type: Boolean,
    default: false
  },
  activeIndex: {
    type: Number,
    default: -1
  }
})

defineEmits(['toggle', 'close', 'select'])
</script>
