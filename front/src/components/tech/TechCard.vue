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
    default: true  // 默认启用玻璃效果
  },
  glassStrong: {
    type: Boolean,
    default: false
  },
  glow: {
    type: Boolean,
    default: true  // 默认启用发光效果
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
  transition: all 0.3s ease;
}

.tech-card.hover-lift:hover {
  border-color: var(--tech-border-subtle);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

.tech-card.glow-border {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow:
    0 0 20px rgba(99, 102, 241, 0.15),
    inset 0 0 20px rgba(99, 102, 241, 0.03);
}

.tech-card.glow-border:hover {
  border-color: rgba(99, 102, 241, 0.6);
  box-shadow:
    0 0 30px rgba(99, 102, 241, 0.25),
    inset 0 0 30px rgba(99, 102, 241, 0.05);
}

.tech-card.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur-lg));
  -webkit-backdrop-filter: blur(var(--glass-blur-lg));
  border: 1px solid var(--glass-border);
}

.dark .tech-card.glass {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
}

.tech-card.glass-strong {
  background: var(--glass-bg-hover);
  backdrop-filter: blur(var(--glass-blur-lg));
  -webkit-backdrop-filter: blur(var(--glass-blur-lg));
  border: 1px solid var(--glass-border);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
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
