<template>
  <picture v-if="srcset" class="responsive-image">
    <!-- AVIF格式 (最优) -->
    <source
      v-if="avifSrc"
      :srcset="avifSrc"
      type="image/avif"
    />
    
    <!-- WebP格式 (次优) -->
    <source
      v-if="webpSrc"
      :srcset="webpSrc"
      type="image/webp"
    />
    
    <!-- 原始格式 (兜底) -->
    <img
      :src="src"
      :alt="alt"
      :loading="loading"
      :decoding="decoding"
      :class="imgClass"
      :style="imgStyle"
      @load="handleLoad"
      @error="handleError"
    />
  </picture>
  
  <img
    v-else
    :src="currentSrc"
    :alt="alt"
    :loading="loading"
    :decoding="decoding"
    :class="imgClass"
    :style="imgStyle"
    @load="handleLoad"
    @error="handleError"
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  // 图片源地址
  src: {
    type: String,
    required: true
  },
  
  // WebP格式地址
  webpSrc: {
    type: String,
    default: ''
  },
  
  // AVIF格式地址
  avifSrc: {
    type: String,
    default: ''
  },
  
  // 响应式图片srcset
  srcset: {
    type: String,
    default: ''
  },
  
  // 替代文本
  alt: {
    type: String,
    required: true
  },
  
  // 加载策略
  loading: {
    type: String,
    default: 'lazy',
    validator: value => ['lazy', 'eager'].includes(value)
  },
  
  // 解码策略
  decoding: {
    type: String,
    default: 'async',
    validator: value => ['async', 'sync', 'auto'].includes(value)
  },
  
  // 占位图
  placeholder: {
    type: String,
    default: ''
  },
  
  // 错误占位图
  errorPlaceholder: {
    type: String,
    default: ''
  },
  
  // 图片类名
  imgClass: {
    type: [String, Array, Object],
    default: ''
  },
  
  // 图片样式
  imgStyle: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['load', 'error'])

// 当前显示的图片地址
const currentSrc = ref(props.placeholder || props.src)
const isLoaded = ref(false)
const hasError = ref(false)

// 图片加载完成
const handleLoad = event => {
  isLoaded.value = true
  currentSrc.value = props.src
  emit('load', event)
}

// 图片加载失败
const handleError = event => {
  hasError.value = true
  if (props.errorPlaceholder) {
    currentSrc.value = props.errorPlaceholder
  }
  emit('error', event)
}

// 预加载图片
const preloadImage = () => {
  if (props.loading === 'eager') {
    const img = new Image()
    img.src = props.src
  }
}

onMounted(() => {
  preloadImage()
})
</script>

<style scoped>
.responsive-image {
  display: block;
  width: 100%;
  height: 100%;
}

.responsive-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
