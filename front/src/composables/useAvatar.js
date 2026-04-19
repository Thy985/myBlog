import { computed } from 'vue'

const DEFAULT_AVATAR = new URL('@/assets/头像.jpg', import.meta.url).href

const FALLBACK_SVG = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"%3E%3Ccircle fill="%23e5e7eb" cx="40" cy="40" r="40"/%3E%3Ctext x="50%25" y="52%25" dominant-baseline="middle" text-anchor="middle" fill="%239ca3af" font-family="sans-serif" font-size="28"%3E?%3C/text%3E%3C/svg%3E'

export function useAvatar(avatar) {
  const avatarUrl = computed(() => {
    if (!avatar?.value && !avatar) {
      return DEFAULT_AVATAR
    }
    const path = avatar?.value ?? avatar
    if (!path) {
      return DEFAULT_AVATAR
    }
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path
    }
    if (path.startsWith('/')) {
      return `${import.meta.env.VITE_APP_BASE_API}${path}`
    }
    return `${import.meta.env.VITE_APP_BASE_API}/${path}`
  })

  const handleAvatarError = (e) => {
    e.target.src = FALLBACK_SVG
  }

  return { avatarUrl, handleAvatarError, DEFAULT_AVATAR, FALLBACK_SVG }
}
