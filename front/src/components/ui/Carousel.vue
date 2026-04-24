<template>
  <div
    class="carousel-container relative overflow-hidden rounded-xl"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <!-- 加载状态 -->
    <div v-if="loading" class="carousel-bg flex items-center justify-center">
      <div class="loading-placeholder">
        <div class="loading-shimmer"></div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!hasItems" class="carousel-bg flex items-center justify-center">
      <div class="empty-placeholder">
        <svg class="w-16 h-16 mb-4 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
        <p class="text-white/60 text-lg font-medium">暂无轮播内容</p>
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
          <!-- 动态渐变占位背景（当没有图片时显示） -->
          <div
            v-if="!hasImage(item)"
            class="absolute inset-0 gradient-placeholder"
            :style="{ background: getGradientPlaceholder(item) }"
          >
            <div class="absolute inset-0 bg-black/20"></div>
            <div class="absolute inset-0 flex items-center justify-center">
              <div class="text-center p-8">
                <div class="w-20 h-20 mx-auto mb-6 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center">
                  <span class="text-white text-3xl font-bold">{{ getTitleInitial(item.title) }}</span>
                </div>
                <h3 class="text-2xl md:text-3xl font-bold text-white mb-2 line-clamp-2">
                  {{ item.title }}
                </h3>
                <p class="text-white/70 text-sm line-clamp-1">{{ item.description || '暂无描述' }}</p>
              </div>
            </div>
          </div>

          <!-- 实际图片（当有图片时显示） -->
          <picture v-if="item.imageWebp && hasImage(item)">
            <source :srcset="item.imageWebp" type="image/webp">
            <img
              :src="item.titleImage || item.imageUrl"
              :srcset="generateSrcSet(item)"
              sizes="(max-width: 640px) 100vw, (max-width: 1024px) 80vw, 1200px"
              class="w-full h-full object-cover carousel-image"
              :alt="item.title"
              width="1200"
              height="400"
              loading="lazy"
              decoding="async"
            />
          </picture>
          <img
            v-else-if="hasImage(item)"
            :src="item.titleImage || item.imageUrl"
            :srcset="generateSrcSet(item)"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 80vw, 1200px"
            class="w-full h-full object-cover carousel-image"
            :alt="item.title"
            width="1200"
            height="400"
            loading="lazy"
            decoding="async"
          />

          <!-- 渐变遮罩（仅图片模式时显示） -->
          <div v-if="hasImage(item)" class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/40 to-transparent"></div>

          <!-- 轮播内容（仅图片模式时显示） -->
          <div v-if="hasImage(item)" class="absolute bottom-0 left-0 right-0 p-6 sm:p-8 md:p-10">
            <span v-if="item.categoryName" class="inline-block px-3 py-1 mb-3 text-xs font-semibold rounded-full bg-white/20 backdrop-blur-sm text-white">
              {{ item.categoryName }}
            </span>
            <h3 class="text-xl sm:text-2xl md:text-3xl font-bold text-white mb-2 line-clamp-2">
              {{ item.title }}
            </h3>
            <p class="text-white/80 text-sm sm:text-base mb-4 line-clamp-2">
              {{ item.description || '暂无描述' }}
            </p>
            <button
              v-if="item.link || item.id"
              class="inline-flex items-center gap-2 px-5 py-2.5 bg-white/20 hover:bg-white/30 text-white rounded-lg transition-all backdrop-blur-sm hover:scale-105"
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

    <!-- 轮播指示器 -->
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
          :class="currentIndex === index ? 'bg-white shadow-lg' : 'bg-white/40 group-hover:bg-white/60'"
        ></span>
      </button>
    </div>

    <!-- 左右箭头 -->
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
        class="h-full progress-bar"
        :style="{ width: `${progress}%` }"
      ></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

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

const currentIndex = ref(0)
const intervalId = ref(null)
const isPlaying = ref(true)
const isPaused = ref(false)
const transitionDuration = ref(500)
const progress = ref(0)
const progressInterval = ref(null)
const touchStartX = ref(0)

const _fallbackImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 400%3E%3Crect fill="%231e293b" width="1200" height="400"/%3E%3C/svg%3E'

const gradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 50%, #4facfe 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 50%, #667eea 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 50%, #fa709a 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 50%, #a18cd1 100%)',
  'linear-gradient(135deg, #6366f1 0%, #ec4899 50%, #f59e0b 100%)',
  'linear-gradient(135deg, #0c3483 0%, #a2b6df 50%, #6b8cce 100%)',
  'linear-gradient(135deg, #ff0844 0%, #ffb199 50%, #ff0844 100%)',
  'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
  'linear-gradient(135deg, #11998e 0%, #38ef7d 50%, #11998e 100%)'
]

const carouselItems = computed(() => props.items || [])
const hasItems = computed(() => carouselItems.value.length > 0)

const hasImage = (item) => {
  if (!item) {return false}
  return !!(item.titleImage || item.imageUrl)
}

const resolveLink = (item) => {
  if (item.link) {return item.link}
  if (item.id) {return `/article/${item.id}`}
  return null
}

const getTitleInitial = (title) => {
  if (!title) {return '?'}
  return title.charAt(0).toUpperCase()
}

const getGradientPlaceholder = (item) => {
  if (!item || !item.id) {return gradients[0]}
  const index = item.id % gradients.length
  return gradients[index]
}

const generateSrcSet = (item) => {
  if (!item.responsiveImages) {return null}
  return item.responsiveImages
    .map(img => `${img.url} ${img.width}w`)
    .join(', ')
}

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

const toggleAutoPlay = () => {
  isPlaying.value = !isPlaying.value
  if (isPlaying.value) {
    startAutoPlay()
  } else {
    stopAutoPlay()
  }
}

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

const handleClick = (item) => {
  const link = resolveLink(item)
  if (!link) {return}
  if (link.startsWith('http')) {
    window.open(link, '_blank')
  } else {
    router.push(link)
  }
}

watch(() => props.items, (newItems) => {
  if (newItems && newItems.length > 0) {
    currentIndex.value = 0
    if (isPlaying.value) {
      startAutoPlay()
    }
  }
}, { immediate: true })

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
  height: 400px;
  position: relative;
}

@media (max-width: 768px) {
  .carousel-bg {
    height: 320px;
  }
}

@media (max-width: 480px) {
  .carousel-bg {
    height: 240px;
  }
}

.gradient-placeholder {
  transition: background 0.5s ease;
}

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

.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  opacity: 0;
  z-index: 10;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.carousel-arrow:hover {
  background-color: rgba(99, 102, 241, 0.6);
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

.carousel-play-control {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  opacity: 0;
  z-index: 10;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.carousel-play-control:hover {
  background-color: rgba(99, 102, 241, 0.6);
  transform: scale(1.1);
}

.carousel-container:hover .carousel-play-control {
  opacity: 1;
}

.progress-bar {
  background: var(--gradient-1);
  transition: width 50ms linear;
}

.empty-placeholder {
  width: 100%;
  height: 100%;
  background: var(--gradient-1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-placeholder {
  width: 100%;
  height: 100%;
  background: var(--bg-secondary);
  position: relative;
  overflow: hidden;
}

.loading-shimmer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.1),
    transparent
  );
  animation: shimmer 1.5s ease-in-out infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@media (max-width: 768px) {
  .carousel-indicators {
    bottom: 12px;
  }

  .carousel-arrow {
    width: 36px;
    height: 36px;
    opacity: 1;
  }

  .carousel-arrow-left {
    left: 8px;
  }

  .carousel-arrow-right {
    right: 8px;
  }

  .carousel-play-control {
    width: 36px;
    height: 36px;
    top: 8px;
    right: 8px;
    opacity: 1;
  }
}

@media (max-width: 480px) {
  .carousel-indicators {
    bottom: 8px;
  }

  .carousel-arrow {
    width: 44px;
    height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .carousel-wrapper {
    transition-duration: 0ms !important;
  }

  .carousel-image,
  .carousel-arrow,
  .carousel-play-control,
  .loading-shimmer {
    transition: none;
    animation: none;
  }

  .gradient-placeholder {
    transition: none;
  }
}
</style>
