<template>
    <footer class="footer-modern">
        <div class="max-w-screen-xl mx-auto px-4 py-16">
            <div class="footer-grid">
                <div class="footer-brand">
                    <a href="/" class="footer-logo">
                        <img
                            :src="logoUrl"
                            class="logo-image"
                            :alt="blogName"
                            width="40"
                            height="40"
                            @error="(e) => e.target.src = defaultLogo"
                        />
                        <span class="logo-text">{{ blogName }}</span>
                    </a>
                    <p class="brand-description">{{ blogDescription }}</p>
                    <div class="social-links">
                        <a v-if="socialLinks.github" :href="socialLinks.github" target="_blank" class="social-link" aria-label="GitHub">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                            </svg>
                        </a>
                        <a v-if="socialLinks.gitee" :href="socialLinks.gitee" target="_blank" class="social-link" aria-label="Gitee">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                                <path d="M11.865 1.513c-4.212 0-7.625 3.413-7.625 7.625 0 4.212 3.413 7.625 7.625 7.625 4.212 0 7.625-3.413 7.625-7.625 0-4.212-3.413-7.625-7.625-7.625zm0 13.593c-3.31 0-6-2.69-6-6s2.69-6 6-6 6 2.69 6 6-2.69 6-6 6zm6.018-3.193H6.085a.482.482 0 0 1-.482-.482V5.854a.482.482 0 0 1 .482-.482h11.798a.482.482 0 0 1 .482.482v5.577a.482.482 0 0 1-.482.482z"/>
                            </svg>
                        </a>
                        <a v-if="socialLinks.csdn" :href="socialLinks.csdn" target="_blank" class="social-link" aria-label="CSDN">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                                <path d="M12 0C5.374 0 0 5.374 0 12c0 6.628 5.374 12 12 12 6.628 0 12-5.372 12-12 0-6.626-5.372-12-12-12zm-1.152 17.273c-3.878-.545-7.007-3.889-7.007-7.92 0-1.322.328-2.562.902-3.655l1.377.796c-.38.728-.595 1.538-.595 2.385 0 2.692 1.838 4.943 4.323 5.632v4.762zm1.152 0v-4.762c2.485-.689 4.323-2.94 4.323-5.632 0-.847-.215-1.657-.595-2.385l1.377-.796c.574 1.093.902 2.333.902 3.655 0 4.031-3.129 7.375-7.007 7.92zm3.802-5.91c-1.63-.943-2.73-2.692-2.73-4.687 0-1.63.743-3.086 1.902-4.007l.828 1.476c-.579.58-.902 1.355-.902 2.207 0 1.816 1.297 3.328 3.076 3.816l-2.174 2.195zm2.074-2.073l-2.174-2.195c.579-.58.902-1.355.902-2.207 0-.828-.323-1.57-.902-2.149l.828-1.476c1.159.921 1.902 2.377 1.902 4.007 0 1.995-1.1 3.744-2.73 4.687l2.074 2.073.002-.022-.002-.022z"/>
                            </svg>
                        </a>
                        <a v-if="socialLinks.zhihu" :href="socialLinks.zhihu" target="_blank" class="social-link" aria-label="知乎">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                                <path d="M8.07 19.153c-.957-.144-1.564-.558-1.564-1.868v-6.943h2.25v-.002l-.001.002h-2.25v-1.564l.001-.002h3.04c.264 0 .518.104.705.29l2.89 2.884 2.89-2.884a1 1 0 0 1 .705-.29h3.04v.002l.001.002h-1.752v1.564h.002l-.001-.002h2.25v6.943c0 1.31-.607 1.724-1.564 1.868-.957.144-2.142.144-3.201 0z"/>
                            </svg>
                        </a>
                        <a v-if="socialLinks.email" :href="'mailto:' + socialLinks.email" class="social-link" aria-label="Email">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                            </svg>
                        </a>
                        <span v-if="!hasSocialLinks" class="social-link-no-data">暂无社交链接</span>
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
                <div class="bottom-left">
                    <span class="copyright">© {{ currentYear }} {{ blogName }}. All rights reserved.</span>
                    <a :href="beianLink" target="_blank" class="beian-link">
                        {{ beianCode }}
                    </a>
                </div>
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

// 计算头像/Logo URL（优先使用用户头像，其次使用博客设置头像）
const logoUrl = computed(() => {
  // 优先使用用户头像
  const userAvatar = store.user?.avatar
  if (userAvatar) {
    if (userAvatar.startsWith('http://') || userAvatar.startsWith('https://')) {
      return userAvatar
    }
    if (userAvatar.startsWith('/')) {
      const baseApi = import.meta.env.VITE_APP_BASE_API.replace(/\/$/, '')
      return `${baseApi}${userAvatar}`
    }
    return `${import.meta.env.VITE_APP_BASE_API}/${userAvatar}`
  }

  // 其次使用博客设置头像
  const settingAvatar = store.setting?.avatar
  if (!settingAvatar) { return defaultLogo }
  if (settingAvatar.startsWith('http://') || settingAvatar.startsWith('https://')) {
    return settingAvatar
  }
  if (settingAvatar.startsWith('/')) {
    const baseApi = import.meta.env.VITE_APP_BASE_API.replace(/\/$/, '')
    return `${baseApi}${settingAvatar}`
  }
  return `${import.meta.env.VITE_APP_BASE_API}/${settingAvatar}`
})

const currentYear = computed(() => new Date().getFullYear())
const blogName = computed(() => store.setting?.blogName || 'XingChen Blog')
const blogDescription = computed(() => store.setting?.description || '专注于分享前沿技术、实战经验和个人成长记录')

const socialLinks = computed(() => ({
    github: store.setting?.github || null,
    gitee: store.setting?.gitee || null,
    csdn: store.setting?.csdn || null,
    zhihu: store.setting?.zhihu || null,
    email: store.setting?.email || null
}))

const hasSocialLinks = computed(() => {
    const links = socialLinks.value
    return links.github || links.gitee || links.csdn || links.zhihu || links.email
})

const beianCode = computed(() => store.setting?.beianCode || '鲁ICP备2026009299号')
const beianLink = computed(() => store.setting?.beianLink || 'https://beian.miit.gov.cn/')

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

.bottom-left {
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: center;
}

@media (min-width: 768px) {
    .bottom-left {
        flex-direction: row;
        gap: 16px;
    }
}

.beian-link {
    color: var(--text-muted);
    text-decoration: none;
    font-size: 12px;
    transition: color 0.2s ease;
}

.beian-link:hover {
    color: var(--color-primary);
}

.copyright {
}

.made-with {
    display: flex;
    align-items: center;
    gap: 6px;
}

.social-link-no-data {
    color: var(--text-muted);
    font-size: 12px;
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
