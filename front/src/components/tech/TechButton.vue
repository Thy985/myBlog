<template>
  <button
    :class="[
      'tech-btn',
      `tech-btn-${variant}`,
      sizeClass,
      { 'glow-border': glow, 'animate-glow-pulse': glow && pulse },
      { 'shimmer': shimmer },
      { 'animate-float': float },
      className
    ]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <!-- Loading spinner -->
    <svg
      v-if="loading"
      class="animate-spin w-4 h-4"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        class="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        stroke-width="4"
      />
      <path
        class="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.196A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>

    <!-- Icon slot -->
    <span v-if="$slots.icon" class="icon-slot">
      <slot name="icon" />
    </span>

    <!-- Default slot -->
    <span v-if="$slots.default">
      <slot />
    </span>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'secondary', 'ghost', 'accent'].includes(v)
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  glow: {
    type: Boolean,
    default: false
  },
  shimmer: {
    type: Boolean,
    default: false
  },
  float: {
    type: Boolean,
    default: false
  },
  pulse: {
    type: Boolean,
    default: false
  },
  className: {
    type: String,
    default: ''
  }
})

defineEmits(['click'])

const sizeClass = computed(() => {
  return {
    sm: 'px-3 py-1.5 text-xs',
    md: 'px-5 py-2.5 text-sm',
    lg: 'px-6 py-3 text-base'
  }[props.size]
})
</script>

<style scoped>
.tech-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
  border: none;
  position: relative;
  overflow: hidden;
}

.tech-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tech-btn:active:not(:disabled) {
  transform: scale(0.98);
}

/* Primary */
.tech-btn-primary {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
  color: white;
  box-shadow: 0 0 20px var(--color-primary-glow);
}

.tech-btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 0 30px var(--color-primary-glow);
}

/* Secondary */
.tech-btn-secondary {
  background: var(--tech-bg-card);
  border: 1px solid var(--tech-border);
  color: var(--tech-text);
}

.tech-btn-secondary:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-subtle);
}

/* Ghost */
.tech-btn-ghost {
  background: transparent;
  color: #F3F4F6;
  border: 1px solid transparent;
}

.tech-btn-ghost:hover:not(:disabled) {
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  border-color: var(--color-primary-glow);
}

/* Accent */
.tech-btn-accent {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
  color: white;
  box-shadow: 0 0 20px var(--color-accent-glow);
}

.tech-btn-accent:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 0 30px var(--color-accent-glow);
}

.icon-slot {
  display: inline-flex;
  align-items: center;
}
</style>
