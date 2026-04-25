<template>
  <div
    :class="[
      'tech-card',
      {
        'glass': glass,
        'glass-strong': glassStrong,
        'glow-border': glow,
        'gradient-border': gradientBorder,
        'noise': noise,
        'hover-lift': hoverLift,
      },
      paddingClass,
      className
    ]"
    @mouseenter="$emit('mouseenter', $event)"
    @mouseleave="$emit('mouseleave', $event)"
  >
    <!-- Header slot -->
    <div v-if="$slots.header" class="card-header">
      <slot name="header" />
    </div>

    <!-- Default slot -->
    <slot />

    <!-- Footer slot -->
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  glass: {
    type: Boolean,
    default: false  // 默认不使用玻璃效果
  },
  glassStrong: {
    type: Boolean,
    default: false
  },
  glow: {
    type: Boolean,
    default: false  // 默认不使用发光效果
  },
  gradientBorder: {
    type: Boolean,
    default: false
  },
  noise: {
    type: Boolean,
    default: false
  },
  hoverLift: {
    type: Boolean,
    default: true
  },
  padding: {
    type: String,
    default: 'md',
    validator: (v) => ['none', 'sm', 'md', 'lg'].includes(v)
  },
  className: {
    type: String,
    default: ''
  }
})

defineEmits(['mouseenter', 'mouseleave'])

const paddingClass = computed(() => {
  return {
    none: '',
    sm: 'p-3',
    md: 'p-5',
    lg: 'p-6'
  }[props.padding]
})
</script>

<style scoped>
.tech-card {
  background: var(--tech-bg-card);
  border: 1px solid var(--tech-border);
  border-radius: var(--radius-xl);
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.tech-card.hover-lift:hover {
  border-color: var(--tech-border-subtle);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

.tech-card.glow-border {
  border-color: var(--color-primary);
  box-shadow: 0 0 12px var(--color-primary-subtle);
}

.tech-card.glow-border:hover {
  border-color: var(--color-primary-hover);
}

.tech-card.glass {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.dark .tech-card.glass {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.tech-card.glass-strong {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
}

.card-header {
  padding-bottom: 1rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid var(--tech-border);
}

.card-footer {
  padding-top: 1rem;
  margin-top: 1rem;
  border-top: 1px solid var(--tech-border);
}
</style>
