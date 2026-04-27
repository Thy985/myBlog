<template>
    <div
        class="article-card"
        role="article"
        :aria-label="article.title"
        data-testid="article-card"
        @mouseenter="handleMouseEnter"
        @mouseleave="handleMouseLeave"
    >
        <div class="card-inner">
            <div class="card-image-wrapper">
                <a class="cursor-pointer block overflow-hidden" @click="goArticleDetail(article.id)">
                    <div class="image-container">
                        <div
                            v-if="!displayImage"
                            class="card-placeholder"
                        >
                            <img
                                :src="getPlaceholderImage()"
                                class="w-full h-full object-cover"
                                :alt="article.title"
                            />
                            <div class="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent"></div>
                            <div class="placeholder-content">
                                <h3 class="text-white font-bold line-clamp-2 px-4">{{ article.title }}</h3>
                            </div>
                        </div>
                        <img
                            v-else
                            ref="imageRef"
                            :data-src="displayImage"
                            class="card-image"
                            :alt="article.title"
                            :src="displayImage"
                            width="800"
                            height="450"
                            loading="lazy"
                            decoding="async"
                            @load="imageLoaded = true"
                            @error="handleImageError"
                        />
                        <div v-if="!imageLoaded && displayImage" class="image-skeleton">
                            <div class="skeleton-shimmer"></div>
                        </div>
                    </div>
                    <div v-if="article.categoryName" class="category-badge">
                        {{ article.categoryName }}
                    </div>
                    <div class="gradient-overlay"></div>
                </a>
            </div>

            <div class="card-content">
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
                        {{ item }}
                    </span>
                </div>

                <a class="cursor-pointer block" @click="goArticleDetail(article.id)">
                    <h2 class="card-title">
                        {{ article.title }}
                    </h2>
                </a>

                <p class="card-description">{{ article.description }}</p>

                <div class="card-footer">
                    <div class="author">
                        <div class="author-avatar">
                            {{ authorInitial }}
                        </div>
                        <span class="author-name">{{ article.authorName || '匿名' }}</span>
                    </div>
                    <div class="meta-right">
                        <span class="meta-item">
                            <svg class="meta-icon" aria-hidden="true" viewBox="0 0 20 20">
                                <path fill="currentColor" d="M5 1v3m5-3v3m5-3v3M1 7h18M5 11h10M2 3h16a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z"/>
                            </svg>
                            {{ formatDate(article.createdTime) }}
                        </span>
                        <span class="meta-item">
                            <svg class="meta-icon" aria-hidden="true" viewBox="0 0 20 20">
                                <path fill="currentColor" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/>
                                <path fill="currentColor" d="M19 12v7a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-7m16-4a2 2 0 0 0-2-2H3a2 2 0 0 0-2 2m16 0a2 2 0 0 0-2-2H3a2 2 0 0 0-2 2"/>
                            </svg>
                            {{ article.readCount || 0 }}
                        </span>
                    </div>
                </div>
            </div>
        </div>
        <div class="card-border-gradient"></div>
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
const isHovered = ref(false)

const fallbackImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 450%3E%3Crect fill="%231e293b" width="800" height="450"/%3E%3C/svg%3E'

// 获取完整的图片URL，处理相对路径
const getImageUrl = (imagePath) => {
    if (!imagePath) {return null}
    if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
        return imagePath
    }
    if (imagePath.startsWith('/')) {
        // 相对路径需要拼接后端地址
        return `http://localhost:8080${imagePath}`
    }
    return `http://localhost:8080/${imagePath}`
}

const getPlaceholderImage = () => {
    if (!props.article.id) {return fallbackImage}
    return `https://picsum.photos/seed/${props.article.id}/800/450`
}

// 主图URL优先使用titleImage，否则用thumbnail，都没有则用placeholder
const displayImage = computed(() => {
    const url = props.article.titleImage || props.article.thumbnail
    return url ? getImageUrl(url) : null
})

const _getTitleInitial = () => {
    const title = props.article.title || ''
    return title.charAt(0).toUpperCase() || '?'
}

const authorInitial = computed(() => {
    const name = props.article.authorName || '匿名'
    return name.charAt(0).toUpperCase()
})

const handleImageError = (e) => {
    // 图片加载失败时使用占位图
    const placeholder = getPlaceholderImage()
    if (e.target.src !== placeholder) {
        e.target.src = placeholder
    }
    imageLoaded.value = true
}

const handleMouseEnter = () => {
    isHovered.value = true
}

const handleMouseLeave = () => {
    isHovered.value = false
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
    position: relative;
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    overflow: hidden;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
    box-shadow: var(--shadow-sm);
}

.article-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-card);
}

.card-inner {
    position: relative;
    z-index: 1;
}

.card-border-gradient {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: var(--gradient-1);
    opacity: 0;
    transform: scaleX(0);
    transform-origin: left;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    z-index: 2;
}

.article-card:hover .card-border-gradient {
    opacity: 1;
    transform: scaleX(1);
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

.card-placeholder {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: flex-end;
}

.placeholder-content {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
}

.card-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease, opacity 0.3s ease;
}

.article-card:hover .card-image {
    transform: scale(1.08);
}

.gradient-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 50%;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.3), transparent);
    opacity: 0;
    transition: opacity var(--transition-normal);
}

.article-card:hover .gradient-overlay {
    opacity: 1;
}

.image-skeleton {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--bg-tertiary);
    animation: pulse 1.5s ease-in-out infinite;
    z-index: 1;
}

.skeleton-shimmer {
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    animation: shimmer 1.5s ease-in-out infinite;
}

@keyframes shimmer {
    0% { transform: translateX(-100%); }
    100% { transform: translateX(100%); }
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.6; }
}

.category-badge {
    position: absolute;
    top: 12px;
    left: 12px;
    padding: 6px 14px;
    background: var(--gradient-1);
    color: white;
    border-radius: var(--radius-full);
    font-size: 12px;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
    z-index: 3;
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
    padding: 4px 10px;
    background: var(--color-primary-subtle);
    color: var(--color-primary);
    border-radius: var(--radius-md);
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    border: 1px solid transparent;
}

.tag-item:hover {
    background: var(--gradient-1);
    color: white;
    transform: scale(1.05);
}

.tag-item:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
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
    transition: color var(--transition-fast);
}

.article-card:hover .card-title {
    color: var(--color-primary);
}

.card-description {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.7;
    margin-bottom: 16px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 16px;
    border-top: 1px solid var(--border-subtle);
}

.author {
    display: flex;
    align-items: center;
    gap: 10px;
}

.author-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--gradient-1);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    color: white;
}

.author-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-secondary);
}

.meta-right {
    display: flex;
    align-items: center;
    gap: 12px;
}

.meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: var(--text-muted);
}

.meta-icon {
    width: 14px;
    height: 14px;
}

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

    .meta-right {
        gap: 8px;
    }

    .meta-item {
        font-size: 12px;
    }
}

@media (prefers-reduced-motion: reduce) {
    .article-card,
    .card-image,
    .gradient-overlay,
    .card-border-gradient,
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

    .article-card:hover .card-title {
        color: var(--text-primary);
    }

    .article-card:hover .card-border-gradient {
        transform: scaleX(0);
    }
}
</style>
