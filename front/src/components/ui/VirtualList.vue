<template>
  <div ref="containerRef" class="virtual-list" :style="containerStyle" @scroll="handleScroll">
    <!-- 占位元素，撑开滚动高度 -->
    <div :style="{ height: `${totalHeight}px`, position: 'relative' }">
      <!-- 可视区域内的元素 -->
      <div
        v-for="item in visibleItems"
        :key="getItemKey(item)"
        :style="{
          position: 'absolute',
          top: `${item._virtualTop}px`,
          left: 0,
          right: 0,
          height: `${itemHeight}px`
        }"
      >
        <slot :item="item.data" :index="item.index"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  // 数据列表
  items: {
    type: Array,
    required: true,
    default: () => []
  },
  
  // 每项高度
  itemHeight: {
    type: Number,
    required: true
  },
  
  // 容器高度
  height: {
    type: [Number, String],
    default: '100%'
  },
  
  // 缓冲区数量(上下各渲染多少个)
  buffer: {
    type: Number,
    default: 5
  },
  
  // 唯一key字段
  keyField: {
    type: String,
    default: 'id'
  }
})

const containerRef = ref(null)
const scrollTop = ref(0)

// 容器样式
const containerStyle = computed(() => ({
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  overflow: 'auto',
  position: 'relative'
}))

// 总高度
const totalHeight = computed(() => props.items.length * props.itemHeight)

// 可视区域高度
const visibleHeight = computed(() => {
  if (!containerRef.value) {
    return 0
  }
  return containerRef.value.clientHeight
})

// 可视区域可容纳的数量
const visibleCount = computed(() => Math.ceil(visibleHeight.value / props.itemHeight))

// 开始索引
const startIndex = computed(() => {
  const index = Math.floor(scrollTop.value / props.itemHeight)
  return Math.max(0, index - props.buffer)
})

// 结束索引
const endIndex = computed(() => {
  const index = startIndex.value + visibleCount.value
  return Math.min(props.items.length, index + props.buffer)
})

// 可视区域内的数据
const visibleItems = computed(() => {
  const items = []
  for (let i = startIndex.value; i < endIndex.value; i++) {
    items.push({
      data: props.items[i],
      index: i,
      _virtualTop: i * props.itemHeight
    })
  }
  return items
})

// 获取item的key
const getItemKey = item => {
  return item.data[props.keyField] || item.index
}

// 滚动处理
const handleScroll = event => {
  scrollTop.value = event.target.scrollTop
}

// 滚动到指定索引
const scrollToIndex = index => {
  if (!containerRef.value) {
    return
  }
  const top = index * props.itemHeight
  containerRef.value.scrollTop = top
}

// 滚动到顶部
const scrollToTop = () => {
  scrollToIndex(0)
}

// 滚动到底部
const scrollToBottom = () => {
  scrollToIndex(props.items.length - 1)
}

// 暴露方法
defineExpose({
  scrollToIndex,
  scrollToTop,
  scrollToBottom
})
</script>

<style scoped>
.virtual-list {
  width: 100%;
}

.virtual-list::-webkit-scrollbar {
  width: 6px;
}

.virtual-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.virtual-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.3);
}
</style>
