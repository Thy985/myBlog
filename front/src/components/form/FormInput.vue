<template>
  <div class="form-group">
    <label 
      v-if="label" 
      :for="inputId" 
      class="block text-sm font-medium text-text-primary mb-2"
    >
      {{ label }}
      <span v-if="required" class="text-error-color ml-1">*</span>
    </label>
    
    <div class="relative">
      <input
        :id="inputId"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :required="required"
        :class="[
          'input',
          {
            'is-valid': isValid && touched,
            'is-invalid': !isValid && touched && errorMessage
          }
        ]"
        :aria-invalid="!isValid && touched"
        :aria-describedby="errorMessage ? `${inputId}-error` : undefined"
        @input="handleInput"
        @blur="handleBlur"
      />
      
      <!-- 验证状态图标 -->
      <div 
        v-if="touched && (isValid || errorMessage)" 
        class="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none"
      >
        <!-- 成功图标 -->
        <svg 
          v-if="isValid" 
          class="w-5 h-5 text-success-color" 
          fill="none" 
          stroke="currentColor" 
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
        </svg>
        
        <!-- 错误图标 -->
        <svg 
          v-else-if="errorMessage" 
          class="w-5 h-5 text-error-color" 
          fill="none" 
          stroke="currentColor" 
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
        </svg>
      </div>
    </div>
    
    <!-- 验证提示信息 -->
    <p 
      v-if="touched && errorMessage" 
      :id="`${inputId}-error`"
      class="form-feedback is-invalid mt-2"
      role="alert"
    >
      {{ errorMessage }}
    </p>
    
    <!-- 成功提示信息 -->
    <p 
      v-else-if="touched && isValid && successMessage" 
      class="form-feedback is-valid mt-2"
    >
      {{ successMessage }}
    </p>
    
    <!-- 帮助文本 -->
    <p 
      v-else-if="helpText" 
      class="text-text-tertiary text-sm mt-2"
    >
      {{ helpText }}
    </p>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

type RuleValidator = (value: string | number) => true | string

const props = defineProps<{
  modelValue?: string | number
  label?: string
  type?: string
  placeholder?: string
  disabled?: boolean
  required?: boolean
  rules?: RuleValidator[]
  helpText?: string
  successMessage?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'validate', valid: boolean): void
}>()

// 生成唯一ID
const inputId = computed(() => `input-${Math.random().toString(36).slice(2, 11)}`)

// 状态
const touched = ref(false)
const errorMessage = ref('')

// 验证状态
const isValid = computed(() => {
  if (!touched.value) {return false}
  return !errorMessage.value && props.modelValue !== ''
})

// 输入处理
const handleInput = (event) => {
  const value = event.target.value
  emit('update:modelValue', value)
  
  // 实时验证
  if (touched.value) {
    validate(value)
  }
}

// 失焦处理
const handleBlur = () => {
  touched.value = true
  validate(props.modelValue)
}

// 验证函数
const validate = (value) => {
  errorMessage.value = ''
  
  // 必填验证
  if (props.required && !value) {
    errorMessage.value = `${props.label || '此字段'}不能为空`
    emit('validate', false)
    return false
  }
  
  // 自定义规则验证
  for (const rule of props.rules) {
    const result = rule(value)
    if (result !== true) {
      errorMessage.value = result
      emit('validate', false)
      return false
    }
  }
  
  emit('validate', true)
  return true
}

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  if (touched.value) {
    validate(newValue)
  }
})

// 暴露验证方法
defineExpose({
  validate: () => {
    touched.value = true
    return validate(props.modelValue)
  },
  reset: () => {
    touched.value = false
    errorMessage.value = ''
  }
})
</script>

<style scoped>
.form-group {
  margin-bottom: 1.5rem;
}

/* 支持减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .input,
  .form-feedback {
    transition: none;
  }
}
</style>
