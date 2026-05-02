<template>
  <div class="password-strength">
    <div class="password-strength-label">密码强度：</div>
    <div class="password-strength-bars">
      <div
        v-for="n in 4"
        :key="n"
        class="password-strength-bar"
        :class="{
          'weak': strength === 'weak' && n <= 1,
          'medium': strength === 'medium' && n <= 2,
          'strong': strength === 'strong' && n <= 3,
          'very-strong': strength === 'very-strong' && n <= 4
        }"
      ></div>
    </div>
    <div class="password-strength-text">{{ strengthText }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  password: {
    type: String,
    default: ''
  }
})

const strength = computed(() => {
  const p = props.password
  if (!p) {return ''}

  let score = 0
  if (p.length >= 6) {score++}
  if (p.length >= 8) {score++}
  if (/[A-Z]/.test(p)) {score++}
  if (/[0-9]/.test(p)) {score++}
  if (/[^A-Za-z0-9]/.test(p)) {score++}

  if (score <= 2) {return 'weak'}
  if (score === 3) {return 'medium'}
  if (score === 4) {return 'strong'}
  return 'very-strong'
})

const strengthText = computed(() => {
  switch (strength.value) {
    case 'weak': return '弱 — 建议增加长度或混合大小写'
    case 'medium': return '中 — 可以更复杂'
    case 'strong': return '强 — 安全性良好'
    case 'very-strong': return '非常强 — 密码强度优秀'
    default: return ''
  }
})
</script>

<style scoped>
.password-strength {
  margin-top: 12px;
  font-size: 12px;
}

.password-strength-label {
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.password-strength-bars {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
}

.password-strength-bar {
  flex: 1;
  height: 4px;
  background: var(--border-color);
  border-radius: 2px;
  transition: background var(--transition-fast);
}

.password-strength-bar.weak {
  background: var(--color-error);
}

.password-strength-bar.medium {
  background: var(--color-warning);
}

.password-strength-bar.strong {
  background: var(--color-success);
}

.password-strength-bar.very-strong {
  background: var(--color-primary);
}

.password-strength-text {
  color: var(--text-muted);
  font-size: 11px;
}
</style>
