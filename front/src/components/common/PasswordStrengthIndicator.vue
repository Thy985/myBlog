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
    case 'weak': return '弱'
    case 'medium': return '中'
    case 'strong': return '强'
    case 'very-strong': return '非常强'
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
  color: #666;
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
  background: #eaeaea;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.password-strength-bar.weak {
  background: #f56c6c;
}

.password-strength-bar.medium {
  background: #e6a23c;
}

.password-strength-bar.strong {
  background: #67c23a;
}

.password-strength-bar.very-strong {
  background: #409eff;
}

.password-strength-text {
  color: #666;
  font-size: 11px;
}
</style>
