<template>
    <section ref="sectionRef" class="featured-section">
        <div class="container">
            <div class="section-header">
                <span class="section-tag">精选内容</span>
                <h2 class="section-title">热门文章</h2>
                <p class="section-desc">阅读量最高、最受欢迎的技术文章</p>
            </div>

            <div v-if="loading" class="articles-grid">
                <div v-for="i in 3" :key="i" class="skeleton-card">
                    <div class="skeleton-image"></div>
                    <div class="skeleton-content">
                        <div class="skeleton-line skeleton-title"></div>
                        <div class="skeleton-line skeleton-text"></div>
                        <div class="skeleton-line skeleton-text short"></div>
                    </div>
                </div>
            </div>

            <div v-else-if="articles.length === 0" class="empty-state">
                <p>暂无精选文章</p>
            </div>

            <div v-else class="articles-grid">
                <article
                    v-for="(article, index) in articles"
                    :key="article.id"
                    class="article-card"
                    :style="{ animationDelay: `${index * 0.1}s` }"
                    :class="{ 'animate-in': isVisible }"
                    @click="$emit('articleClick', article.id)"
                >
                    <div class="card-image-wrapper">
                        <div class="image-container">
                            <img
                                v-if="article.titleImage"
                                :src="article.titleImage"
                                :alt="article.title"
                                class="card-image"
                                loading="lazy"
                            />
                            <div v-else class="card-placeholder">
                                <img
                                    :src="`https://picsum.photos/seed/${article.id}/800/450`"
                                    class="w-full h-full object-cover"
                                    :alt="article.title"
                                />
                                <div class="placeholder-overlay">
                                    <h3 class="text-white font-bold line-clamp-2">{{ article.title }}</h3>
                                </div>
                            </div>
                        </div>
                        <span v-if="article.categoryName" class="category-badge">
                            {{ article.categoryName }}
                        </span>
                    </div>

                    <div class="card-content">
                        <div class="article-meta">
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

                        <h3 class="card-title">{{ article.title }}</h3>
                        <p class="card-excerpt">{{ article.description }}</p>

                        <div class="card-footer">
                            <div class="author">
                                <div class="author-avatar">{{ getInitial(article.authorName) }}</div>
                                <span class="author-name">{{ article.authorName || '匿名' }}</span>
                            </div>
                            <span class="read-time">{{ calculateReadTime(article.content) }} 分钟阅读</span>
                        </div>
                    </div>
                    <div class="card-border-gradient"></div>
                </article>
            </div>
        </div>
    </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

defineProps({
    articles: {
        type: Array,
        default: () => []
    },
    loading: {
        type: Boolean,
        default: false
    }
})

defineEmits(['articleClick'])

const sectionRef = ref(null)
const isVisible = ref(false)
let observer = null

onMounted(() => {
    if (!sectionRef.value) {return}

    observer = new IntersectionObserver(
        (entries) => {
            if (entries[0].isIntersecting) {
                isVisible.value = true
                // 动画触发后断开观察，防止重复触发
                if (observer) {
                    observer.disconnect()
                    observer = null
                }
            }
        },
        { threshold: 0.1 }
    )

    observer.observe(sectionRef.value)
})

onUnmounted(() => {
    if (observer) {
        observer.disconnect()
        observer = null
    }
})

const formatDate = (dateStr) => {
    if (!dateStr) {return ''}
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const getInitial = (name) => {
    return (name || '匿').charAt(0).toUpperCase()
}

const calculateReadTime = (content) => {
    if (!content) {return 3}
    const words = content.length
    return Math.max(1, Math.ceil(words / 500))
}
</script>

<style scoped>
.featured-section {
    padding: 80px 0;
}

.section-header {
    text-align: center;
    margin-bottom: 48px;
}

.section-tag {
    display: inline-block;
    padding: 6px 12px;
    background: var(--color-primary-subtle);
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    color: var(--color-primary);
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 16px;
}

.section-title {
    font-size: 36px;
    font-weight: 700;
    margin-bottom: 12px;
    background: var(--gradient-1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.section-desc {
    font-size: 16px;
    color: var(--text-secondary);
}

.articles-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px;
}

@media (max-width: 1024px) {
    .articles-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 640px) {
    .articles-grid {
        grid-template-columns: 1fr;
    }
}

.article-card {
    position: relative;
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    overflow: hidden;
    cursor: pointer;
    opacity: 0;
    transform: translateY(30px);
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.article-card.animate-in {
    opacity: 1;
    transform: translateY(0);
}

.article-card:hover {
    transform: translateY(-8px);
    box-shadow: var(--shadow-card);
}

.card-border-gradient {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: var(--gradient-1);
    opacity: 0;
    transition: opacity var(--transition-normal);
}

.article-card:hover .card-border-gradient {
    opacity: 1;
}

.card-image-wrapper {
    position: relative;
}

.image-container {
    position: relative;
    aspect-ratio: 16 / 9;
    overflow: hidden;
}

.card-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
}

.article-card:hover .card-image {
    transform: scale(1.08);
}

.card-placeholder {
    position: relative;
    width: 100%;
    height: 100%;
}

.placeholder-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
    background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
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
}

.card-content {
    padding: 20px;
}

.article-meta {
    display: flex;
    gap: 16px;
    margin-bottom: 12px;
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

.card-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    transition: color var(--transition-fast);
}

.article-card:hover .card-title {
    color: var(--color-primary);
}

.card-excerpt {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin-bottom: 16px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
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
    gap: 8px;
}

.author-avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--gradient-1);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: white;
}

.author-name {
    font-size: 14px;
    color: var(--text-secondary);
}

.read-time {
    font-size: 13px;
    color: var(--text-muted);
}

.skeleton-card {
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    overflow: hidden;
}

.skeleton-image {
    aspect-ratio: 16 / 9;
    background: var(--bg-tertiary);
    animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-content {
    padding: 20px;
}

.skeleton-line {
    height: 16px;
    background: var(--bg-tertiary);
    border-radius: 4px;
    margin-bottom: 12px;
    animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-title {
    height: 20px;
    width: 80%;
}

.skeleton-text {
    width: 100%;
}

.skeleton-text.short {
    width: 60%;
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}

.empty-state {
    text-align: center;
    padding: 60px 20px;
    color: var(--text-muted);
}

@media (prefers-reduced-motion: reduce) {
    .article-card {
        opacity: 1;
        transform: none;
        transition: none;
    }

    .article-card:hover {
        transform: none;
    }

    .article-card:hover .card-image {
        transform: none;
    }

    .skeleton-image,
    .skeleton-line {
        animation: none;
    }
}
</style>
