<template>
    <section ref="heroRef" class="hero-section">
        <div class="hero-bg">
            <div class="hero-grid"></div>
            <div class="hero-orb hero-orb-1"></div>
            <div class="hero-orb hero-orb-2"></div>
        </div>

        <div class="hero-content">
            <div class="hero-badge" :class="{ 'animate-fade-in': isVisible }">
                <span class="badge-dot"></span>
                <span class="typewriter-text" :class="{ 'typing': isVisible }">AI 驱动的智能博客系统</span>
            </div>

            <h1 class="hero-title" :class="{ 'animate-fade-in': isVisible }">
                探索<span class="gradient-text">技术</span>的<br>
                无限可能
            </h1>

            <p class="hero-description" :class="{ 'animate-fade-in': isVisible }">
                这里分享前沿的技术见解、深入的项目分析和实用的开发技巧。<br class="hide-mobile">
                无论是前端开发、后端架构还是 AI 应用，都能找到有价值的内容。
            </p>

            <div class="hero-actions" :class="{ 'animate-fade-in': isVisible }">
                <button class="btn btn-primary btn-large" @click="$emit('browseArticles')">
                    浏览文章
                </button>
                <button class="btn btn-secondary btn-large" @click="$emit('learnMore')">
                    了解更多
                </button>
            </div>

            <div v-if="hasRealStats" class="hero-stats" :class="{ 'animate-fade-in': isVisible }">
                <div v-if="stats.articleCount > 0" class="stat-item">
                    <span class="stat-value">{{ stats.articleCount }}</span>
                    <span class="stat-label">技术文章</span>
                </div>
                <div v-if="stats.articleCount > 0 && stats.totalViews > 0" class="stat-divider"></div>
                <div v-if="stats.totalViews > 0" class="stat-item">
                    <span class="stat-value">{{ formatCount(stats.totalViews) }}</span>
                    <span class="stat-label">总阅读量</span>
                </div>
                <div v-if="stats.totalViews > 0 && stats.subscriberCount > 0" class="stat-divider"></div>
                <div v-if="stats.subscriberCount > 0" class="stat-item">
                    <span class="stat-value">{{ formatCount(stats.subscriberCount) }}</span>
                    <span class="stat-label">活跃读者</span>
                </div>
            </div>
        </div>

        <div class="scroll-indicator" :class="{ 'animate-fade-in': isVisible }">
            <div class="mouse">
                <div class="wheel"></div>
            </div>
            <span>向下滚动</span>
        </div>
    </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'

const props = defineProps({
    stats: {
        type: Object,
        default: () => ({
            articleCount: 128,
            totalViews: 50000,
            subscriberCount: 2800
        })
    }
})

defineEmits(['browseArticles', 'learnMore'])

const heroRef = ref(null)
const isVisible = ref(false)

const hasRealStats = computed(() => {
    const { articleCount, totalViews, subscriberCount } = props.stats || {}
    return articleCount > 0 || totalViews > 0 || subscriberCount > 0
})

const formatCount = (num) => {
    if (num >= 10000) {
        return (num / 10000).toFixed(1) + 'W+'
    }
    if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K+'
    }
    return num.toString()
}

let observer = null

onMounted(() => {
    if (!heroRef.value) {return}

    observer = new IntersectionObserver(
        (entries) => {
            if (entries[0].isIntersecting) {
                isVisible.value = true
                if (observer) {
                    observer.disconnect()
                    observer = null
                }
            }
        },
        { threshold: 0.1 }
    )

    observer.observe(heroRef.value)
})

onUnmounted(() => {
    if (observer) {
        observer.disconnect()
        observer = null
    }
})
</script>

<style scoped>
.hero-section {
    position: relative;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 120px 24px 80px;
    overflow: hidden;
}

.hero-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 0;
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.08) 0%, rgba(236, 72, 153, 0.05) 50%, rgba(245, 158, 11, 0.03) 100%);
}

.hero-grid {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image:
        linear-gradient(rgba(99, 102, 241, 0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(99, 102, 241, 0.03) 1px, transparent 1px);
    background-size: 60px 60px;
    mask-image: radial-gradient(ellipse 80% 60% at 50% 40%, black 20%, transparent 80%);
    -webkit-mask-image: radial-gradient(ellipse 80% 60% at 50% 40%, black 20%, transparent 80%);
}

.hero-orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.4;
    animation: float 20s ease-in-out infinite;
}

.hero-orb-1 {
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(99, 102, 241, 0.3) 0%, transparent 70%);
    top: -100px;
    right: -100px;
}

.hero-orb-2 {
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(236, 72, 153, 0.2) 0%, transparent 70%);
    bottom: -50px;
    left: -100px;
    animation-delay: -10s;
}

@keyframes float {
    0%, 100% { transform: translate(0, 0) scale(1); }
    25% { transform: translate(20px, -20px) scale(1.05); }
    50% { transform: translate(-10px, 10px) scale(0.95); }
    75% { transform: translate(15px, 15px) scale(1.02); }
}

.hero-content {
    position: relative;
    z-index: 1;
    text-align: center;
    max-width: 900px;
    width: 100%;
}

.hero-badge {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background: rgba(99, 102, 241, 0.1);
    border: 1px solid rgba(99, 102, 241, 0.3);
    border-radius: 50px;
    font-size: 13px;
    font-weight: 500;
    color: var(--color-primary);
    margin-bottom: 24px;
    opacity: 0;
    transform: translateY(20px);
}

.hero-badge.animate-fade-in {
    opacity: 1;
    transform: translateY(0);
    transition: opacity 0.6s cubic-bezier(0.22, 1, 0.36, 1), transform 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.typewriter-text {
    overflow: hidden;
    white-space: nowrap;
    border-right: 2px solid var(--color-primary);
    animation: blink 0.8s step-end infinite;
}

.typewriter-text.typing {
    animation: typing 1.5s steps(20, end), blink 0.8s step-end 1.5s;
    border-right-color: transparent;
}

@keyframes typing {
    from { width: 0; }
    to { width: 100%; }
}

@keyframes blink {
    50% { border-color: var(--color-primary); }
}

.badge-dot {
    width: 8px;
    height: 8px;
    background: var(--color-primary);
    border-radius: 50%;
    animation: pulse 2s ease-in-out infinite;
    flex-shrink: 0;
}

@keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.5; transform: scale(1.2); }
}

.hero-title {
    font-size: clamp(40px, 8vw, 72px);
    font-weight: 800;
    line-height: 1.1;
    margin-bottom: 24px;
    letter-spacing: -2px;
    opacity: 0;
    transform: translateY(30px);
}

.hero-title.animate-fade-in {
    opacity: 1;
    transform: translateY(0);
    transition: opacity 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.1s, transform 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.1s;
}

.gradient-text {
    background: var(--gradient-1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.hero-description {
    font-size: 18px;
    color: var(--text-secondary);
    max-width: 600px;
    margin: 0 auto 40px;
    line-height: 1.8;
    opacity: 0;
    transform: translateY(30px);
}

.hero-description.animate-fade-in {
    opacity: 1;
    transform: translateY(0);
    transition: opacity 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.2s, transform 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.2s;
}

.hero-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
    flex-wrap: wrap;
    margin-bottom: 60px;
    opacity: 0;
    transform: translateY(30px);
}

.hero-actions.animate-fade-in {
    opacity: 1;
    transform: translateY(0);
    transition: opacity 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.3s, transform 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.3s;
}

.hero-stats {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 32px;
    padding-top: 40px;
    border-top: 1px solid var(--border-color);
    opacity: 0;
    transform: translateY(30px);
}

.hero-stats.animate-fade-in {
    opacity: 1;
    transform: translateY(0);
    transition: opacity 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.4s, transform 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.4s;
}

.stat-item {
    text-align: center;
}

.stat-value {
    display: block;
    font-size: 32px;
    font-weight: 700;
    background: var(--gradient-1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.stat-label {
    font-size: 14px;
    color: var(--text-muted);
}

.stat-divider {
    width: 1px;
    height: 40px;
    background: var(--border-color);
}

.scroll-indicator {
    position: absolute;
    bottom: 40px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: var(--text-muted);
    font-size: 12px;
    opacity: 0;
    transition: opacity 0.6s ease 0.6s;
}

.scroll-indicator.animate-fade-in {
    opacity: 1;
}

.mouse {
    width: 24px;
    height: 40px;
    border: 2px solid var(--border-color);
    border-radius: 12px;
    position: relative;
}

.wheel {
    width: 4px;
    height: 8px;
    background: var(--color-primary);
    border-radius: 2px;
    position: absolute;
    top: 8px;
    left: 50%;
    transform: translateX(-50%);
    animation: scroll 2s ease-in-out infinite;
}

@keyframes scroll {
    0%, 100% { opacity: 1; top: 8px; }
    50% { opacity: 0; top: 20px; }
}

.btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 14px 28px;
    border-radius: 12px;
    font-weight: 600;
    font-size: 15px;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    text-decoration: none;
    border: none;
}

.btn-primary {
    background: var(--gradient-1);
    color: white;
    box-shadow: var(--shadow-primary);
}

.btn-primary:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 25px rgba(99, 102, 241, 0.5);
}

.btn-secondary {
    background: var(--bg-card);
    color: var(--text-primary);
    border: 1px solid var(--border-color);
}

.btn-secondary:hover {
    background: var(--bg-tertiary);
    border-color: var(--color-primary);
    transform: translateY(-2px);
}

@media (max-width: 768px) {
    .hero-section {
        padding: 100px 20px 60px;
    }

    .hero-stats {
        gap: 20px;
    }

    .stat-value {
        font-size: 24px;
    }

    .stat-divider {
        height: 30px;
    }

    .hide-mobile {
        display: none;
    }

    .scroll-indicator {
        display: none;
    }

    .hero-orb {
        display: none;
    }
}

@media (prefers-reduced-motion: reduce) {
    .hero-orb {
        animation: none;
    }

    .badge-dot {
        animation: none;
    }

    .wheel {
        animation: none;
    }

    .typewriter-text {
        animation: none;
        border-right-color: transparent;
    }

    .hero-badge,
    .hero-title,
    .hero-description,
    .hero-actions,
    .hero-stats,
    .scroll-indicator {
        opacity: 1;
        transform: none;
        transition: none;
    }
}
</style>
