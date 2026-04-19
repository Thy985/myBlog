<template>
  <div 
    class="carousel-container relative overflow-hidden rounded-xl shadow-lg"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <!-- 加载状态 -->
    <div v-if="loading" class="carousel-bg flex items-center justify-center bg-background-secondary">
      <div class="animate-pulse flex flex-col items-center gap-3">
        <div class="w-12 h-12 rounded-full bg-background-tertiary"></div>
        <div class="w-32 h-4 rounded bg-background-tertiary"></div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!hasItems" class="carousel-bg flex items-center justify-center bg-background-secondary">
      <div class="text-center text-text-tertiary">
        <svg class="w-12 h-12 mx-auto mb-2 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
        <p>暂无轮播内容</p>
      </div>
    </div>

    <!-- 轮播图片容器 -->
    <div
      v-else
      class="carousel-wrapper flex overflow-hidden"
      :style="{ transform: `translateX(-${currentIndex * 100}%)`, transitionDuration: `${transitionDuration}ms` }"
    >
      <div 
        v-for="(item, index) in carouselItems" 
        :key="item.id || index"
        class="carousel-item flex-shrink-0 w-full relative"
      >
        <div class="carousel-bg relative overflow-hidden">
          <!-- 响应式图片 -->
          <picture v-if="item.imageWebp">
            <source :srcset="item.imageWebp" type="image/webp">
            <img
              :src="resolveImageUrl(item)"
              :srcset="generateSrcSet(item)"
              sizes="(max-width: 640px) 100vw, (max-width: 1024px) 80vw, 1200px"
              class="w-full h-full object-cover carousel-image"
              :alt="item.title"
              width="1200"
              height="400"
              loading="lazy"
              decoding="async"
              @error="handleImageError(item)"
            />
          </picture>
          <img
            v-else
            :src="resolveImageUrl(item)"
            :srcset="generateSrcSet(item)"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 80vw, 1200px"
            class="w-full h-full object-cover carousel-image"
            :alt="item.title"
            width="1200"
            height="400"
            loading="lazy"
            decoding="async"
            @error="handleImageError(item)"
          />
          
          <!-- 渐变遮罩 -->
          <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/40 to-transparent"></div>
          
          <!-- 轮播内容 -->
          <div class="absolute bottom-0 left-0 right-0 p-6 sm:p-8 md:p-10">
            <span v-if="item.tag" class="inline-block px-2 py-1 mb-2 text-xs font-medium bg-primary-color text-white rounded">
              {{ item.tag }}
            </span>
            <h3 class="text-xl sm:text-2xl md:text-3xl font-bold text-white mb-2 line-clamp-2">
              {{ item.title }}
            </h3>
            <p class="text-gray-200 text-sm sm:text-base mb-4 line-clamp-2">
              {{ item.description || '暂无描述' }}
            </p>
            <button 
              v-if="item.link"
              class="inline-flex items-center gap-2 px-4 py-2 bg-white/20 hover:bg-white/30 text-white rounded-lg transition-colors backdrop-blur-sm"
              @click="handleClick(item)"
            >
              查看详情
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 轮播指示器 - 改进点击区域 -->
    <div v-if="hasItems && carouselItems.length > 1" class="carousel-indicators absolute bottom-4 left-0 right-0 flex justify-center gap-2">
      <button 
        v-for="(item, index) in carouselItems" 
        :key="index"
        class="group relative p-1"
        :aria-label="`跳转到第${index + 1}张`"
        @click="goToSlide(index)"
      >
        <span 
          class="block w-8 h-1.5 rounded-full transition-all duration-300"
          :class="currentIndex === index ? 'bg-white' : 'bg-white/40 group-hover:bg-white/60'"
        ></span>
      </button>
    </div>
    
    <!-- 左右箭头 - 悬停显示 -->
    <template v-if="hasItems && carouselItems.length > 1">
      <button 
        class="carousel-arrow carousel-arrow-left"
        aria-label="上一张"
        @click="prevSlide"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>
      <button 
        class="carousel-arrow carousel-arrow-right"
        aria-label="下一张"
        @click="nextSlide"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
        </svg>
      </button>
    </template>
    
    <!-- 轮播暂停/播放控制按钮 -->
    <button 
      v-if="hasItems && carouselItems.length > 1"
      class="carousel-play-control"
      :aria-label="isPlaying ? '暂停轮播' : '播放轮播'"
      @click="toggleAutoPlay"
    >
      <svg v-if="isPlaying" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
    </button>

    <!-- 进度条 -->
    <div v-if="hasItems && isPlaying && carouselItems.length > 1" class="absolute bottom-0 left-0 right-0 h-1 bg-white/20">
      <div 
        class="h-full bg-primary-color transition-all ease-linear"
        :style="{ width: `${progress}%` }"
      ></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import logger from '@/utils/logger'

const router = useRouter()

// Props
const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  autoplay: {
    type: Boolean,
    default: true
  },
  interval: {
    type: Number,
    default: 5000
  },
  loading: {
    type: Boolean,
    default: false
  }
})

// 响应式数据
const currentIndex = ref(0)
const intervalId = ref(null)
const isPlaying = ref(true)
const isPaused = ref(false)
const transitionDuration = ref(500)
const progress = ref(0)
const progressInterval = ref(null)
const touchStartX = ref(0)

// 占位图
const placeholderImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 400"%3E%3Crect fill="%23f3f4f6" width="1200" height="400"/%3E%3Ctext fill="%239ca3af" font-family="sans-serif" font-size="24" dy="10.5" font-weight="bold" x="50%25" y="50%25" text-anchor="middle"%3E暂无图片%3C/text%3E%3C/svg%3E'
const fallbackImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 400"%3E%3Crect fill="%23e5e7eb" width="1200" height="400"/%3E%3Ctext fill="%236b7280" font-family="sans-serif" font-size="20" dy="10.5" x="50%25" y="50%25" text-anchor="middle"%3E图片加载失败%3C/text%3E%3C/svg%3E'

const errorIndexSet = new Set()

const carouselItems = computed(() => props.items || [])
const hasItems = computed(() => carouselItems.value.length > 0)

function resolveImageUrl(item) {
  if (errorIndexSet.has(item.id)) {return fallbackImage}
  return item.titleImage || item.imageUrl || placeholderImage
}

function resolveLink(item) {
  if (item.link) {return item.link}
  if (item.id) {return `/article/${item.id}`}
  return null
}

// 生成响应式图片srcset
const generateSrcSet = (item) => {
  if (!item.responsiveImages) {return null}
  return item.responsiveImages
    .map(img => `${img.url} ${img.width}w`)
    .join(', ')
}

// 方法
const nextSlide = () => {
  if (!hasItems.value) {return}
  currentIndex.value = (currentIndex.value + 1) % carouselItems.value.length
  resetProgress()
}

const prevSlide = () => {
  if (!hasItems.value) {return}
  currentIndex.value = (currentIndex.value - 1 + carouselItems.value.length) % carouselItems.value.length
  resetProgress()
}

const goToSlide = (index) => {
  currentIndex.value = index
  resetProgress()
}

// 自动播放控制
const startAutoPlay = () => {
  if (!props.autoplay || !isPlaying.value || isPaused.value || !hasItems.value) {return}
  
  stopAutoPlay()
  intervalId.value = setInterval(nextSlide, props.interval)
  startProgress()
}

const stopAutoPlay = () => {
  if (intervalId.value) {
    clearInterval(intervalId.value)
    intervalId.value = null
  }
  stopProgress()
}

// 进度条动画
const startProgress = () => {
  stopProgress()
  progress.value = 0
  const step = 100 / (props.interval / 50)
  progressInterval.value = setInterval(() => {
    progress.value = Math.min(progress.value + step, 100)
  }, 50)
}

const stopProgress = () => {
  if (progressInterval.value) {
    clearInterval(progressInterval.value)
    progressInterval.value = null
  }
  progress.value = 0
}

const resetProgress = () => {
  if (isPlaying.value && !isPaused.value) {
    startProgress()
  }
}

// 切换轮播状态
const toggleAutoPlay = () => {
  isPlaying.value = !isPlaying.value
  if (isPlaying.value) {
    startAutoPlay()
  } else {
    stopAutoPlay()
  }
}

// 鼠标悬停控制
const handleMouseEnter = () => {
  isPaused.value = true
  stopAutoPlay()
}

const handleMouseLeave = () => {
  isPaused.value = false
  if (isPlaying.value) {
    startAutoPlay()
  }
}

// 触摸滑动支持
const handleTouchStart = (e) => {
  touchStartX.value = e.touches[0].clientX
  stopAutoPlay()
}

const handleTouchEnd = (e) => {
  const touchEndX = e.changedTouches[0].clientX
  const diff = touchStartX.value - touchEndX
  
  if (Math.abs(diff) > 50) {
    if (diff > 0) {
      nextSlide()
    } else {
      prevSlide()
    }
  }
  
  if (isPlaying.value) {
    startAutoPlay()
  }
}

// 点击处理
const handleClick = (item) => {
  const link = resolveLink(item)
  if (!link) {return}
  if (link.startsWith('http')) {
    window.open(link, '_blank')
  } else {
    router.push(link)
  }
}

// 图片错误处理（使用 Set 追踪，不修改 props/computed 数据）
const handleImageError = (item) => {
  if (item?.id) {
    errorIndexSet.add(item.id)
  }
  logger.error(`轮播图图片加载失败: ${item?.title || item?.id || 'unknown'}`)
}

// 监听items变化
watch(() => props.items, (newItems) => {
  if (newItems && newItems.length > 0) {
    currentIndex.value = 0
    if (isPlaying.value) {
      startAutoPlay()
    }
  }
}, { immediate: true })

// 生命周期
onMounted(() => {
  if (props.autoplay && hasItems.value) {
    startAutoPlay()
  }
})

onUnmounted(() => {
  stopAutoPlay()
})
</script>

<style scoped>
.carousel-container {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.carousel-wrapper {
  display: flex;
  transition-property: transform;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}

.carousel-item {
  flex-shrink: 0;
  width: 100%;
}

.carousel-bg {
  height: 320px;
  position: relative;
}

/* 图片懒加载时的淡入效果 */
.carousel-image {
  opacity: 0;
  transition: opacity 0.3s ease;
}

.carousel-image[src]:not([src=""]) {
  opacity: 1;
}

.carousel-indicators {
  position: absolute;
  bottom: 16px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 4px;
}

/* 箭头按钮 - 默认隐藏，悬停显示 */
.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.3);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  z-index: 10;
}

.carousel-arrow:hover {
  background-color: rgba(0, 0, 0, 0.5);
  transform: translateY(-50%) scale(1.1);
}

.carousel-container:hover .carousel-arrow {
  opacity: 1;
}

.carousel-arrow-left {
  left: 16px;
}

.carousel-arrow-right {
  right: 16px;
}

/* 播放/暂停控制按钮 */
.carousel-play-control {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.4);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  z-index: 10;
  backdrop-filter: blur(4px);
}

.carousel-play-control:hover {
  background-color: rgba(0, 0, 0, 0.6);
  transform: scale(1.1);
}

.carousel-container:hover .carousel-play-control {
  opacity: 1;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .carousel-bg {
    height: 240px;
  }
  
  .carousel-indicators {
    bottom: 12px;
  }
  
  .carousel-arrow {
    width: 32px;
    height: 32px;
    opacity: 1;
  }
  
  .carousel-arrow-left {
    left: 8px;
  }
  
  .carousel-arrow-right {
    right: 8px;
  }
  
  .carousel-play-control {
    width: 32px;
    height: 32px;
    top: 8px;
    right: 8px;
    opacity: 1;
  }
}

@media (max-width: 480px) {
  .carousel-bg {
    height: 200px;
  }
  
  .carousel-indicators {
    bottom: 8px;
  }
  
  .carousel-arrow {
    width: 28px;
    height: 28px;
  }
}

/* 减少动画偏好支持 */
@media (prefers-reduced-motion: reduce) {
  .carousel-wrapper {
    transition-duration: 0ms !important;
  }
  
  .carousel-image {
    transition: none;
  }
  
  .carousel-arrow,
  .carousel-play-control {
    transition: none;
  }
}
</style>
