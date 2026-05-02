<template>
  <div class="relative">
    <button
      id="user-menu-button"
      type="button"
      class="flex items-center gap-2 px-3 py-2 rounded-lg transition-all duration-300 hover:bg-primary-subtle focus:outline-none focus:ring-2 focus:ring-primary-color/20 group"
      :aria-expanded="isOpen"
      aria-haspopup="true"
      aria-controls="user-menu"
      @click="$emit('toggle')"
    >
      <img
        class="w-8 h-8 rounded-full object-cover transition-all duration-300 group-hover:scale-110"
        :src="avatarUrl"
        :alt="username"
        loading="lazy"
        width="32"
        height="32"
        @error="handleAvatarError"
      >
      <svg
        class="w-4 h-4 text-text-tertiary transition-all duration-300"
        :class="{ 'rotate-180': isOpen }"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
      </svg>
    </button>

    <!-- 用户下拉菜单 -->
    <div
      v-if="isOpen"
      id="user-menu"
      class="absolute right-0 mt-2 w-56 bg-background-primary border border-border-color rounded-lg shadow-xl dark:bg-background-primary transition-all duration-300 transform origin-top-right animate-fadeIn"
      role="menu"
      aria-labelledby="user-menu-button"
    >
      <ul class="py-1">
        <li>
          <a
            class="flex items-center px-4 py-3 text-sm text-text-secondary hover:bg-primary-subtle hover:text-primary-color transition-all duration-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-color/20"
            role="menuitem"
            @click="$router.push('/user')"
          >
            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
              <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm-7 9a7 7 0 1 1 14 0H3Z" />
            </svg>
            个人中心
          </a>
        </li>
        <li>
          <a
            class="flex items-center px-4 py-3 text-sm text-text-secondary hover:bg-primary-subtle hover:text-primary-color transition-all duration-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-color/20"
            role="menuitem"
            @click="$router.push('/user/articles/create')"
          >
            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
              <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10H7m0 0a2 2 0 1 0 0 4h6a2 2 0 1 0 0-4ZM7 10a2 2 0 0 1 2-2h6a2 2 0 1 1 2 2m0 0a2 2 0 1 0 0 4H9a2 2 0 1 0 0-4Z" />
            </svg>
            发布文章
          </a>
        </li>
        <li>
          <a
            class="flex items-center px-4 py-3 text-sm text-text-secondary hover:bg-primary-subtle hover:text-primary-color transition-all duration-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-color/20"
            role="menuitem"
            @click="$router.push('/user/settings')"
          >
            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
              <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" />
            </svg>
            设置
          </a>
        </li>
        <li>
          <a
            class="flex items-center px-4 py-3 text-sm text-text-secondary hover:bg-primary-subtle hover:text-primary-color transition-all duration-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-color/20"
            role="menuitem"
            @click="$emit('logout')"
          >
            <svg class="w-5 h-5 mr-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 16 16">
              <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8h11m0 0-4-4m4 4-4 4m-5 3H3a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h3" />
            </svg>
            退出登录
          </a>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { useAvatar } from '@/composables/useAvatar'

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  },
  username: {
    type: String,
    default: ''
  },
  avatar: {
    type: String,
    default: ''
  },
  isAdmin: {
    type: Boolean,
    default: false
  }
})

defineEmits(['toggle', 'logout'])

const { avatarUrl, handleAvatarError } = useAvatar(() => props.avatar)
</script>
