<template>
  <span
    :class="[
      'tech-badge',
      `tech-badge-${variant}`,
      sizeClass,
      { 'tech-badge-glow': glow },
      { 'tech-badge-dot': dot },
      className
    ]"
  >
    <!-- Dot indicator -->
    <span v-if="dot" class="badge-dot" :class="`dot-${variant}`" />

    <!-- Icon slot -->
    <span v-if="$slots.icon" class="badge-icon">
      <slot name="icon" />
    </span>

    <!-- Default slot -->
    <slot />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'accent'
  size?: 'sm' | 'md' | 'lg'
  glow?: boolean
  dot?: boolean
  className?: string
}>()

const sizeClass = computed(() => {
  return {
    sm: 'px-2 py-0.5 text-[10px]',
    md: 'px-2.5 py-1 text-xs',
    lg: 'px-3 py-1.5 text-sm'
  }[props.size]
})
</script>

<style scoped>
.tech-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 9999px;
  font-weight: 500;
  border: 1px solid transparent;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

/* Primary */
.tech-badge-primary {
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  border-color: rgba(99, 102, 241, 0.3);
}

.tech-badge-primary:hover {
  background: rgba(99, 102, 241, 0.25);
}

/* Secondary */
.tech-badge-secondary {
  background: var(--color-accent-subtle);
  color: var(--color-accent);
  border-color: var(--color-accent-glow);
}

/* Success */
.tech-badge-success {
  background: var(--color-success-subtle);
  color: var(--color-success);
  border-color: rgba(52, 211, 153, 0.3);
}

/* Warning */
.tech-badge-warning {
  background: var(--color-warning-subtle);
  color: var(--color-warning);
  border-color: rgba(245, 158, 11, 0.3);
}

/* Error */
.tech-badge-error {
  background: var(--color-error-subtle);
  color: var(--color-error);
  border-color: rgba(248, 113, 113, 0.3);
}

/* Accent */
.tech-badge-accent {
  background: var(--color-accent-subtle);
  color: var(--color-accent);
  border-color: rgba(96, 165, 250, 0.3);
}

/* Glow effect */
.tech-badge-glow {
  box-shadow: 0 0 15px currentColor;
}

.tech-badge-glow.tech-badge-primary {
  box-shadow: 0 0 15px var(--color-primary-glow);
}

.tech-badge-glow.tech-badge-success {
  box-shadow: 0 0 15px var(--color-success-glow);
}

.tech-badge-glow.tech-badge-error {
  box-shadow: 0 0 15px var(--color-error-glow);
}

/* Dot indicator */
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-primary {
  background: var(--color-primary);
  box-shadow: 0 0 6px var(--color-primary-glow);
}

.dot-secondary {
  background: var(--color-accent);
  box-shadow: 0 0 6px var(--color-accent-glow);
}

.dot-success {
  background: var(--color-success);
  box-shadow: 0 0 6px var(--color-success-glow);
}

.dot-warning {
  background: var(--color-warning);
  box-shadow: 0 0 6px var(--color-warning-glow);
}

.dot-error {
  background: var(--color-error);
  box-shadow: 0 0 6px var(--color-error-glow);
}

.dot-accent {
  background: var(--color-accent);
  box-shadow: 0 0 6px var(--color-accent-glow);
}

/* Icon */
.badge-icon {
  display: flex;
  align-items: center;
}
</style>
