<template>
    <footer class="footer-glass mt-auto">
        <div class="max-w-screen-xl mx-auto px-4 py-8">
            <!-- 主要内容区 -->
            <div class="grid grid-cols-1 md:grid-cols-3 gap-8 mb-6">
                <!-- Logo 和简介 -->
                <div class="footer-brand">
                    <div class="flex items-center gap-3 mb-3">
                        <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-primary-color to-accent-color flex items-center justify-center shadow-glow-sm">
                            <span class="text-white text-lg font-bold">{{ logoLetter }}</span>
                        </div>
                        <div>
                            <h3 class="text-text-primary font-semibold text-base">{{ blogName }}</h3>
                            <p class="text-text-muted text-xs">{{ blogDescription }}</p>
                        </div>
                    </div>
                    <p class="text-text-muted text-sm leading-relaxed">
                        {{ blogDescription }}
                    </p>
                </div>

                <!-- 快速链接 -->
                <div class="footer-links">
                    <h4 class="text-text-primary font-medium text-sm mb-3">快速链接</h4>
                    <ul class="space-y-2">
                        <li>
                            <a href="/" class="footer-link">首页</a>
                        </li>
                        <li>
                            <a href="/category/list" class="footer-link">分类</a>
                        </li>
                        <li>
                            <a href="/tag/list" class="footer-link">标签</a>
                        </li>
                        <li>
                            <a href="/archives" class="footer-link">归档</a>
                        </li>
                    </ul>
                </div>

                <!-- 联系方式 -->
                <div class="footer-contact">
                    <h4 class="text-text-primary font-medium text-sm mb-3">联系方式</h4>
                    <div class="flex items-center gap-4">
                        <a
                            :href="githubUrl"
                            target="_blank"
                            rel="noopener noreferrer"
                            class="social-link"
                            aria-label="GitHub"
                        >
                            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                            </svg>
                        </a>
                        <a
                            href="mailto:contact@example.com"
                            class="social-link"
                            aria-label="邮箱"
                        >
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                            </svg>
                        </a>
                    </div>
                </div>
            </div>

            <!-- 底部版权栏 -->
            <div class="flex flex-col md:flex-row items-center justify-between gap-4 pt-6 border-t border-border-subtle">
                <span class="text-text-muted text-xs">
                    © {{ currentYear }} XingChen Blog. All rights reserved.
                </span>

                <!-- 返回顶部按钮 -->
                <button
                    v-if="showBackToTop"
                    class="back-to-top-btn"
                    aria-label="返回顶部"
                    @click="scrollToTop"
                >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"/>
                    </svg>
                    <span>返回顶部</span>
                </button>
            </div>
        </div>
    </footer>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useMainStore } from '@/stores'

const store = useMainStore()
const scrollY = ref(0)

const currentYear = computed(() => new Date().getFullYear())
const blogName = computed(() => store.setting?.blogName || 'XingChen Blog')
const blogDescription = computed(() => store.setting?.description || '记录学习，分享成长')
const githubUrl = computed(() => store.setting?.github || 'https://github.com')
const email = computed(() => store.setting?.email || 'mailto:contact@example.com')
const logoLetter = computed(() => blogName.value.charAt(0).toUpperCase() || 'X')

const showBackToTop = computed(() => scrollY.value > 300)

let rafId = null
const handleScroll = () => {
    if (rafId !== null) {return}
    rafId = requestAnimationFrame(() => {
        scrollY.value = window.scrollY
        rafId = null
    })
}

const scrollToTop = () => {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    })
}

onMounted(() => {
    window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
    if (rafId !== null) {
        cancelAnimationFrame(rafId)
        rafId = null
    }
})
</script>

<style scoped>
.footer-glass {
    background: var(--glass-bg);
    backdrop-filter: blur(var(--glass-blur-lg));
    -webkit-backdrop-filter: blur(var(--glass-blur-lg));
    border-top: 1px solid var(--glass-border);

    @supports not (backdrop-filter: blur(20px)) {
        background: var(--bg-secondary);
        border-color: var(--border-color);
    }
}

.dark .footer-glass {
    background: rgba(10, 10, 11, 0.9);
    border-top: 1px solid var(--glass-border);

    @supports (backdrop-filter: blur(20px)) {
        backdrop-filter: blur(var(--glass-blur-lg));
        -webkit-backdrop-filter: blur(var(--glass-blur-lg));
    }

    @supports not (backdrop-filter: blur(20px)) {
        background: var(--bg-primary);
        border-color: var(--border-color);
    }
}

.footer-link {
    display: block;
    color: var(--text-muted);
    font-size: 0.875rem;
    transition: color var(--transition-fast);
    text-decoration: none;
}

.footer-link:hover {
    color: var(--color-primary);
}

.social-link {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 2.25rem;
    height: 2.25rem;
    border-radius: var(--radius-md);
    color: var(--text-muted);
    transition: all var(--transition-fast);
}

.social-link:hover {
    color: var(--color-primary);
    background: var(--color-primary-subtle);
    transform: translateY(-2px);
}

.back-to-top-btn {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 1rem;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    background: var(--bg-primary);
    color: var(--text-secondary);
    font-size: 0.875rem;
    cursor: pointer;
    transition: all var(--transition-fast);
}

.back-to-top-btn:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
    background: var(--color-primary-subtle);
    transform: translateY(-2px);
}

.shadow-glow-sm {
    box-shadow: 0 0 12px var(--color-primary-glow);
}
</style>
