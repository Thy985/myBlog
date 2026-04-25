<template>
  <!-- 移动端目录导航按钮 -->
  <div class="lg:hidden mb-4">
    <button
      class="flex items-center justify-between w-full p-3 bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
      @click="$emit('toggle')"
    >
      <span class="font-medium flex items-center gap-2">
        <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
        </svg>
        文章目录
      </span>
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>
  </div>

  <!-- 移动端目录抽屉导航 -->
  <Teleport to="body">
    <Transition
      enter-active-class="transition ease-out duration-300"
      enter-from-class="translate-x-full"
      enter-to-class="translate-x-0"
      leave-active-class="transition ease-in duration-200"
      leave-from-class="translate-x-0"
      leave-to-class="translate-x-full"
    >
      <div
        v-if="visible"
        class="lg:hidden fixed inset-0 z-50"
      >
        <!-- 遮罩层 -->
        <div
          class="absolute inset-0 bg-black/30"
          @click="$emit('close')"
        ></div>

        <!-- 抽屉内容 -->
        <div class="absolute inset-y-0 right-0 w-4/5 max-w-sm bg-white dark:bg-gray-800 shadow-2xl border-l border-gray-200 dark:border-gray-700 flex flex-col">
          <!-- 抽屉头部 -->
          <div class="p-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
              <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
              </svg>
              文章目录
            </h3>
            <button
              class="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-500 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors flex items-center justify-center"
              @click="$emit('close')"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 目录列表 -->
          <div class="flex-1 overflow-y-auto p-4">
            <ul v-if="tocItems.length > 0" class="space-y-2">
              <li v-for="(item, index) in tocItems" :key="item.id || `toc-${index}`">
                <a
                  :href="'#' + item.id"
                  :class="[
                    'block py-3 px-4 rounded-lg transition-all duration-200',
                    activeIndex === index
                      ? 'bg-primary text-white shadow-md'
                      : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
                  ]"
                  :style="{ paddingLeft: (item.level - 2) * 16 + 16 + 'px' }"
                  @click.prevent="$emit('select', item.id, index)"
                >
                  <span class="flex items-center gap-2">
                    <span :class="['w-1.5 h-1.5 rounded-full', activeIndex === index ? 'bg-white' : 'bg-gray-400']"></span>
                    <span class="truncate">{{ item.title }}</span>
                  </span>
                </a>
              </li>
            </ul>
            <div v-else class="text-center py-12 text-gray-500 dark:text-gray-400">
              <svg class="w-12 h-12 mx-auto mb-3 text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              <p>暂无目录</p>
            </div>
          </div>

          <!-- 抽屉底部 -->
          <div class="p-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
            <button
              class="w-full py-2 px-4 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors font-medium"
              @click="$emit('close')"
            >
              关闭目录
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
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
