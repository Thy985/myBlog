<template>
    <div
        class="article-card"
        data-testid="article-card"
        @mouseenter="handleMouseEnter"
        @mouseleave="handleMouseLeave"
    >
        <!-- 图片区域 -->
        <div class="card-image-wrapper">
            <a class="cursor-pointer block overflow-hidden rounded-t-xl" @click="goArticleDetail(article.id)">
                <div class="image-container">
                    <img
                        ref="imageRef"
                        :data-src="article.titleImage"
                        class="card-image"
                        :alt="article.title"
                        :src="article.titleImage || placeholderImage"
                        width="800"
                        height="450"
                        loading="lazy"
                        decoding="async"
                        @load="imageLoaded = true"
                        @error="handleImageError"
                    />
                    <div v-if="!imageLoaded" class="image-skeleton">
                        <div class="skeleton-shimmer"></div>
                    </div>
                </div>
                <!-- 分类标签覆盖在图片上 -->
                <div v-if="article.categoryName" class="category-badge">
                    {{ article.categoryName }}
                </div>
            </a>
        </div>

        <!-- 内容区域 -->
        <div class="card-content">
            <!-- 标签区域 - 改进视觉层次 -->
            <div v-if="displayTags.length > 0" class="tags-section">
                <span
                    v-for="item in displayTags"
                    :key="item"
                    class="tag-item"
                    tabindex="0"
                    role="button"
                    :aria-label="`查看标签 ${item} 的文章`"
                    @click="goTagArticleListPage(null, item)"
                    @keydown.enter="goTagArticleListPage(null, item)"
                >
                    <span class="tag-hash">#</span>{{ item }}
                </span>
            </div>

            <!-- 标题 -->
            <a class="cursor-pointer block" @click="goArticleDetail(article.id)">
                <h2 class="card-title">
                    {{ article.title }}
                </h2>
            </a>

            <!-- 描述 -->
            <p class="card-description">{{ article.description }}</p>

            <!-- meta 信息 -->
            <div class="card-meta">
                <div class="meta-left">
                    <span class="meta-item">
                        <svg class="meta-icon" aria-hidden="true" viewBox="0 0 20 20">
                            <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M5 1v3m5-3v3m5-3v3M1 7h18M5 11h10M2 3h16a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" />
                        </svg>
                        {{ formatDate(article.createdTime) }}
                    </span>
                    <span class="meta-item">
                        <svg class="meta-icon" aria-hidden="true" viewBox="0 0 20 20">
                            <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                            <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M19 12v7a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-7m16-4a2 2 0 0 0-2-2H3a2 2 0 0 0-2 2m16 0V5a2 2 0 0 0-2-2H3a2 2 0 0 0-2 2v3m16 4v-3m0 0V5m0 4a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2m16 0a2 2 0 0 0-2-2H3a2 2 0 0 0-2 2" />
                        </svg>
                        {{ article.readCount || 0 }}
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { defineProps, defineEmits, computed, ref } from 'vue'

const props = defineProps({
    article: {
        type: Object,
        required: true
    }
})

const emit = defineEmits([
    'goArticleDetail',
    'goTagArticleListPage',
    'goCategoryArticleListPage'
])

const imageRef = ref(null)
const imageLoaded = ref(false)
const placeholderImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 450"%3E%3Crect fill="%23f3f4f6" width="800" height="450"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" fill="%239ca3af" font-family="sans-serif" font-size="24"%3E暂无封面%3C/text%3E%3C/svg%3E'
const fallbackImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 450"%3E%3Crect fill="%23e5e7eb" width="800" height="450"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" fill="%236b7280" font-family="sans-serif" font-size="20"%3E图片加载失败%3C/text%3E%3C/svg%3E'

const handleImageError = (e) => {
    if (e.target.src !== fallbackImage) {
        e.target.src = fallbackImage
    }
    imageLoaded.value = true
}

const displayTags = computed(() => {
    let tags = props.article.tagNames
    if (typeof tags === 'string') {
        tags = tags.split(/\s+/).filter(tag => tag.trim() !== '')
    } else if (!Array.isArray(tags)) {
        tags = []
    }
    return tags.slice(0, 3)
})

const formatDate = (dateStr) => {
    if (!dateStr) {return ''}
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const goArticleDetail = (articleId) => {
    emit('goArticleDetail', articleId)
}

const goTagArticleListPage = (tagId, tagName) => {
    emit('goTagArticleListPage', tagId, tagName)
}
</script>

<style scoped>
.article-card {
    /* 玻璃拟态效果 - 半透明磨砂 */
    background: var(--glass-bg);
    border-radius: var(--radius-xl);
    overflow: hidden;
    transition: transform var(--transition-normal), box-shadow var(--transition-normal), border-color var(--transition-normal);
    border: 1px solid var(--glass-border);
    box-shadow: var(--shadow-md);

    /* 玻璃拟态 - 支持时启用 */
    @supports (backdrop-filter: blur(12px)) {
        backdrop-filter: blur(var(--glass-blur));
        -webkit-backdrop-filter: blur(var(--glass-blur));
    }

    /* 不支持时的降级方案 */
    @supports not (backdrop-filter: blur(12px)) {
        background: var(--bg-card);
        border-color: var(--border-color);
    }
}

.article-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);
    border-color: var(--color-primary-subtle);
}

.card-image-wrapper {
    position: relative;
    overflow: hidden;
}

.image-container {
    position: relative;
    width: 100%;
    aspect-ratio: 16 / 9;
    background-color: var(--bg-tertiary);
    overflow: hidden;
}

.card-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease, opacity 0.3s ease;
}

.article-card:hover .card-image {
    transform: scale(1.05);
}

/* 图片骨架屏 */
.image-skeleton {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(90deg, var(--bg-tertiary) 25%, var(--bg-secondary) 50%, var(--bg-tertiary) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    z-index: 1;
}

.skeleton-shimmer {
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
    animation: shimmer-slide 1.5s infinite;
}

@keyframes shimmer {
    0% {
        background-position: -200% 0;
    }
    100% {
        background-position: 200% 0;
    }
}

@keyframes shimmer-slide {
    0% {
        transform: translateX(-100%);
    }
    100% {
        transform: translateX(100%);
    }
}

.category-badge {
    position: absolute;
    top: 12px;
    left: 12px;
    background: linear-gradient(135deg, var(--color-primary) 0%, #8B5CF6 100%);
    color: white;
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    box-shadow: 0 2px 12px var(--color-primary-glow);
    z-index: 2;
}

.card-content {
    padding: 20px;
}

.tags-section {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
}

.tag-item {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    padding: 4px 10px;
    background: var(--color-primary-subtle);
    color: var(--color-primary);
    border-radius: var(--radius-sm);
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    border: 1px solid transparent;
}

.tag-item:hover {
    background: var(--color-primary);
    color: white;
    transform: scale(1.05);
    box-shadow: 0 0 10px var(--color-primary-glow);
}

.tag-hash {
    opacity: 0.6;
    font-weight: 400;
}

.card-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
    line-height: 1.4;
    margin-bottom: 10px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    transition: color 0.2s ease;
}

.article-card:hover .card-title {
    color: var(--color-primary);
}

.card-description {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin-bottom: 16px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.card-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 16px;
    border-top: 1px solid var(--glass-border);
}

.meta-left {
    display: flex;
    align-items: center;
    gap: 16px;
}

.meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--text-muted);
}

.meta-icon {
    width: 16px;
    height: 16px;
}

/* 响应式优化 */
@media (max-width: 640px) {
    .card-content {
        padding: 16px;
    }
    
    .card-title {
        font-size: 16px;
    }
    
    .card-description {
        font-size: 13px;
        -webkit-line-clamp: 2;
    }
}

/* 减少动画偏好支持 */
@media (prefers-reduced-motion: reduce) {
    .article-card,
    .card-image,
    .tag-item {
        transition: none;
    }
    
    .image-skeleton,
    .skeleton-shimmer {
        animation: none;
    }
    
    .article-card:hover {
        transform: none;
    }
    
    .article-card:hover .card-image {
        transform: none;
    }
}
</style>
