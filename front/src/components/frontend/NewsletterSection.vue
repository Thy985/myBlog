<template>
    <section ref="sectionRef" class="newsletter-section">
        <div class="newsletter-card">
            <div class="card-glow card-glow-1"></div>
            <div class="card-glow card-glow-2"></div>

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
                        :disabled="isSubmitting"
                    />
                    <button type="submit" class="newsletter-btn" :disabled="isSubmitting">
                        <span v-if="isSubmitting">订阅中...</span>
                        <span v-else>立即订阅</span>
                    </button>
                </form>
            </div>

            <div v-if="showSuccess" class="success-message">
                <svg class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
                <span>订阅成功！感谢您的支持。</span>
            </div>
        </div>
    </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['subscribe'])

const sectionRef = ref(null)
const isVisible = ref(false)
const email = ref('')
const isSubmitting = ref(false)
const showSuccess = ref(false)

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
        { threshold: 0.2 }
    )

    observer.observe(sectionRef.value)
})

onUnmounted(() => {
    if (observer) {
        observer.disconnect()
        observer = null
    }
})

const handleSubscribe = async () => {
    if (!email.value || isSubmitting.value) {return}

    isSubmitting.value = true

    try {
        await new Promise(resolve => setTimeout(resolve, 1000))

        emit('subscribe', email.value)

        showSuccess.value = true
        email.value = ''

        setTimeout(() => {
            showSuccess.value = false
        }, 3000)
    } catch (error) {
        console.error('Subscription failed:', error)
    } finally {
        isSubmitting.value = false
    }
}
</script>

<style scoped>
.newsletter-section {
    padding: 60px 24px;
}

.newsletter-card {
    position: relative;
    max-width: 900px;
    margin: 0 auto;
    padding: 48px;
    background: var(--gradient-subtle);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-2xl);
    overflow: hidden;
}

.card-glow {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.4;
    pointer-events: none;
}

.card-glow-1 {
    top: -100px;
    left: -100px;
    width: 300px;
    height: 300px;
    background: radial-gradient(circle, rgba(99, 102, 241, 0.3) 0%, transparent 70%);
}

.card-glow-2 {
    bottom: -100px;
    right: -100px;
    width: 300px;
    height: 300px;
    background: radial-gradient(circle, rgba(236, 72, 153, 0.3) 0%, transparent 70%);
}

.newsletter-content {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 32px;
}

@media (min-width: 768px) {
    .newsletter-content {
        flex-direction: row;
        justify-content: space-between;
        text-align: left;
    }
}

.newsletter-text {
    flex: 1;
}

.newsletter-title {
    font-size: 28px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
}

.newsletter-desc {
    font-size: 16px;
    color: var(--text-secondary);
    margin: 0;
}

.newsletter-form {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
    max-width: 400px;
}

@media (min-width: 480px) {
    .newsletter-form {
        flex-direction: row;
    }
}

.newsletter-input {
    flex: 1;
    padding: 14px 18px;
    background: var(--bg-primary);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-lg);
    color: var(--text-primary);
    font-size: 15px;
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

.newsletter-input:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.newsletter-btn {
    padding: 14px 28px;
    background: var(--gradient-1);
    border: none;
    border-radius: var(--radius-lg);
    color: white;
    font-weight: 600;
    font-size: 15px;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    white-space: nowrap;
}

.newsletter-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: var(--shadow-primary);
}

.newsletter-btn:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
}

.success-message {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-top: 24px;
    padding: 12px 20px;
    background: rgba(16, 185, 129, 0.1);
    border: 1px solid rgba(16, 185, 129, 0.3);
    border-radius: var(--radius-lg);
    color: var(--color-success);
    font-size: 14px;
    font-weight: 500;
    animation: fadeIn 0.3s ease;
}

.success-icon {
    width: 20px;
    height: 20px;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 640px) {
    .newsletter-card {
        padding: 32px 24px;
    }

    .newsletter-title {
        font-size: 24px;
    }
}

@media (prefers-reduced-motion: reduce) {
    .newsletter-btn:hover:not(:disabled) {
        transform: none;
    }

    .success-message {
        animation: none;
    }
}
</style>
