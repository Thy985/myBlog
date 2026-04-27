<template>
    <footer class="footer-modern">
        <div class="max-w-screen-xl mx-auto px-4 py-16">
            <div class="footer-grid">
                <div class="footer-brand">
                    <a href="/" class="footer-logo">
                        <img
                            :src="store.setting.avatar || defaultLogo"
                            class="logo-image"
                            :alt="blogName"
                            width="40"
                            height="40"
                        />
                        <span class="logo-text">{{ blogName }}</span>
                    </a>
                    <p class="brand-description">{{ blogDescription }}</p>
                    <div class="social-links">
                        <a href="#" class="social-link" aria-label="GitHub">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                            </svg>
                        </a>
                        <a href="#" class="social-link" aria-label="Twitter">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                                <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
                            </svg>
                        </a>
                        <a href="#" class="social-link" aria-label="RSS">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                                <path d="M6.18 15.64a2.18 2.18 0 0 1 2.18 2.18C8.36 19.01 7.38 20 6.18 20C4.98 20 4 19.01 4 17.82a2.18 2.18 0 0 1 2.18-2.18M4 4.44A15.56 15.56 0 0 1 19.56 20h-2.83A12.73 12.73 0 0 0 4 7.27V4.44m0 5.66a9.9 9.9 0 0 1 9.9 9.9h-2.83A7.07 7.07 0 0 0 4 12.93V10.1z"/>
                            </svg>
                        </a>
                        <a href="mailto:contact@example.com" class="social-link" aria-label="Email">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                            </svg>
                        </a>
                    </div>
                </div>

                <div class="footer-column">
                    <h4 class="column-title">快速链接</h4>
                    <ul class="footer-links">
                        <li><a href="/" class="footer-link">首页</a></li>
                        <li><a href="/category" class="footer-link">分类</a></li>
                        <li><a href="/tag" class="footer-link">标签</a></li>
                        <li><a href="/archive" class="footer-link">归档</a></li>
                        <li><a href="/about" class="footer-link">关于</a></li>
                    </ul>
                </div>

                <div class="footer-column">
                    <h4 class="column-title">内容分类</h4>
                    <ul class="footer-links">
                        <li v-for="cat in categories" :key="cat.id">
                            <router-link :to="`/category/${cat.id}`" class="footer-link">
                                {{ cat.name }}
                                <span v-if="cat.articleCount" class="text-xs text-gray-400">({{ cat.articleCount }})</span>
                            </router-link>
                        </li>
                        <li v-if="categories.length === 0">
                            <span class="footer-link text-gray-400">暂无分类</span>
                        </li>
                    </ul>
                </div>

                <div class="footer-column">
                    <h4 class="column-title">热门标签</h4>
                    <div class="tags-cloud">
                        <router-link
                            v-for="tag in tags"
                            :key="tag.id"
                            :to="`/tag/${tag.id}`"
                            class="tag"
                        >
                            {{ tag.name || tag.tag_name }}
                        </router-link>
                        <span v-if="tags.length === 0" class="text-gray-400 text-sm">暂无标签</span>
                    </div>
                </div>
            </div>

            <div class="newsletter-section">
                <div class="newsletter-content">
                    <div class="newsletter-text">
                        <h3 class="newsletter-title">订阅更新</h3>
                        <p class="newsletter-desc">订阅我们的 newsletter，获取最新文章和技术资讯</p>
                    </div>
                    <form class="newsletter-form" @submit.prevent="handleSubscribe">
                        <input
                            v-model="email"
                            type="email"
                            class="newsletter-input"
                            placeholder="输入你的邮箱地址"
                            required
                        />
                        <button type="submit" class="newsletter-btn">
                            订阅
                        </button>
                    </form>
                </div>
            </div>

            <div class="footer-bottom">
                <span class="copyright">© {{ currentYear }} {{ blogName }}. All rights reserved.</span>
                <span class="made-with">
                    Made with
                    <span class="heart">❤️</span>
                    using Vue 3
                </span>
            </div>
        </div>
    </footer>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useMainStore } from '@/stores'

const props = defineProps({
    categories: {
        type: Array,
        default: () => []
    },
    tags: {
        type: Array,
        default: () => []
    }
})

const store = useMainStore()

const defaultLogo = new URL('@/assets/头像.jpg', import.meta.url).href
const email = ref('')

const currentYear = computed(() => new Date().getFullYear())
const blogName = computed(() => store.setting?.blogName || 'XingChen Blog')
const blogDescription = computed(() => store.setting?.description || '专注于分享前沿技术、实战经验和个人成长记录')

const handleSubscribe = () => {
    if (email.value) {
        alert(`感谢订阅！我们会将更新发送到 ${email.value}`)
        email.value = ''
    }
}
</script>

<style scoped>
.footer-modern {
    background: var(--bg-secondary);
    border-top: 1px solid var(--border-color);
    margin-top: auto;
}

.footer-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 40px;
    margin-bottom: 48px;
}

@media (min-width: 768px) {
    .footer-grid {
        grid-template-columns: 2fr 1fr 1fr 1fr;
    }
}

.footer-brand {
    max-width: 320px;
}

.footer-logo {
    display: flex;
    align-items: center;
    gap: 12px;
    text-decoration: none;
    margin-bottom: 16px;
}

.logo-image {
    width: 40px;
    height: 40px;
    border-radius: var(--radius-lg);
    object-fit: cover;
}

.logo-text {
    font-size: 20px;
    font-weight: 700;
    background: var(--gradient-1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.brand-description {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.7;
    margin-bottom: 20px;
}

.social-links {
    display: flex;
    gap: 12px;
}

.social-link {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: var(--radius-lg);
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    color: var(--text-secondary);
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.social-link svg {
    width: 18px;
    height: 18px;
}

.social-link:hover {
    background: var(--gradient-1);
    border-color: transparent;
    color: white;
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
}

.social-link:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
}

.footer-column {
}

.column-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 20px;
}

.footer-links {
    list-style: none;
    padding: 0;
    margin: 0;
}

.footer-links li {
    margin-bottom: 12px;
}

.footer-link {
    color: var(--text-secondary);
    font-size: 14px;
    text-decoration: none;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    display: inline-block;
}

.footer-link:hover {
    color: var(--color-primary);
    transform: translateX(4px);
}

.tags-cloud {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.tag {
    display: inline-block;
    padding: 6px 12px;
    background: var(--color-primary-subtle);
    border: 1px solid transparent;
    border-radius: var(--radius-md);
    font-size: 13px;
    color: var(--color-primary);
    text-decoration: none;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.tag:hover {
    background: var(--color-primary);
    color: white;
    transform: translateY(-2px);
}

.newsletter-section {
    padding: 40px;
    background: var(--gradient-subtle);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-xl);
    margin-bottom: 32px;
}

.newsletter-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
    align-items: center;
    text-align: center;
}

@media (min-width: 768px) {
    .newsletter-content {
        flex-direction: row;
        justify-content: space-between;
        text-align: left;
    }
}

.newsletter-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
}

.newsletter-desc {
    font-size: 14px;
    color: var(--text-secondary);
    margin: 0;
}

.newsletter-form {
    display: flex;
    gap: 12px;
    width: 100%;
    max-width: 400px;
}

.newsletter-input {
    flex: 1;
    padding: 12px 16px;
    background: var(--bg-primary);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-lg);
    color: var(--text-primary);
    font-size: 14px;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.newsletter-input:focus {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px var(--color-primary-subtle);
}

.newsletter-input::placeholder {
    color: var(--text-muted);
}

.newsletter-btn {
    padding: 12px 24px;
    background: var(--gradient-1);
    border: none;
    border-radius: var(--radius-lg);
    color: white;
    font-weight: 600;
    font-size: 14px;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    white-space: nowrap;
}

.newsletter-btn:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-primary);
}

.footer-bottom {
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: center;
    padding-top: 24px;
    border-top: 1px solid var(--border-subtle);
    font-size: 14px;
    color: var(--text-muted);
}

@media (min-width: 768px) {
    .footer-bottom {
        flex-direction: row;
        justify-content: space-between;
    }
}

.copyright {
}

.made-with {
    display: flex;
    align-items: center;
    gap: 6px;
}

.heart {
    animation: heartbeat 1.5s ease-in-out infinite;
}

@keyframes heartbeat {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.2); }
}

@media (prefers-reduced-motion: reduce) {
    .social-link,
    .footer-link,
    .tag,
    .newsletter-btn {
        transition: none;
    }

    .social-link:hover,
    .tag:hover,
    .newsletter-btn:hover {
        transform: none;
    }

    .heart {
        animation: none;
    }
}
</style>
