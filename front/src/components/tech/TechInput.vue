<template>
  <div class="tech-input-wrapper">
    <!-- Label -->
    <label
      v-if="label"
      :for="inputId"
      class="tech-input-label"
    >
      {{ label }}
      <span v-if="required" class="text-tech-error">*</span>
    </label>

    <!-- Input container with icon -->
    <div class="input-container" :class="{ 'has-icon': $slots.icon }">
      <!-- Prefix icon -->
      <span v-if="$slots.icon" class="input-icon input-icon-prefix">
        <slot name="icon" />
      </span>

      <input
        :id="inputId"
        v-model="model"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :class="[
          'tech-input',
          inputClass,
          {
            'tech-input-neon': neon,
            'has-error': error,
            'has-icon-prefix': $slots.icon,
            'has-icon-suffix': $slots['icon-suffix'],
          }
        ]"
        @focus="$emit('focus', $event)"
        @blur="$emit('blur', $event)"
        @input="$emit('input', $event)"
      />

      <!-- Suffix icon -->
      <span v-if="$slots['icon-suffix']" class="input-icon input-icon-suffix">
        <slot name="icon-suffix" />
      </span>
    </div>

    <!-- Error message -->
    <p v-if="error" class="tech-input-error">
      {{ error }}
    </p>

    <!-- Help text -->
    <p v-else-if="helpText" class="tech-input-help">
      {{ helpText }}
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  required: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  helpText: {
    type: String,
    default: ''
  },
  neon: {
    type: Boolean,
    default: false
  },
  inputClass: {
    type: String,
    default: ''
  },
  id: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'focus', 'blur', 'input'])

const model = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const inputId = computed(() => props.id || `tech-input-${Math.random().toString(36).slice(2, 9)}`)
</script>

<style scoped>
.tech-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tech-input-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--tech-text-secondary);
}

.tech-input-label .text-tech-error {
  color: var(--color-error);
  margin-left: 2px;
}

.input-container {
  position: relative;
  display: flex;
  align-items: center;
}

.tech-input {
  width: 100%;
  background: var(--tech-bg-card);
  border: 1px solid var(--tech-border);
  border-radius: var(--radius-lg);
  color: var(--tech-text);
  padding: 10px 14px;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.tech-input::placeholder {
  color: var(--tech-text-muted);
}

.tech-input:hover:not(:disabled) {
  border-color: var(--tech-border-subtle);
}

.tech-input:focus:not(:disabled) {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.tech-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Neon style */
.tech-input.tech-input-neon {
  background: var(--tech-bg);
  border-color: var(--tech-border-subtle);
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.tech-input.tech-input-neon:hover:not(:disabled) {
  border-color: rgba(99, 102, 241, 0.5);
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.1);
}

.tech-input.tech-input-neon:focus:not(:disabled) {
  border-color: var(--color-primary);
  box-shadow:
    0 0 0 3px rgba(99, 102, 241, 0.15),
    0 0 20px rgba(99, 102, 241, 0.2);
}

/* Error state */
.tech-input.has-error {
  border-color: var(--color-error);
}

.tech-input.has-error:focus:not(:disabled) {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15);
}

/* Icon styles */
.input-icon {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  color: var(--tech-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
}

.input-icon-prefix {
  left: 12px;
}

.input-icon-suffix {
  right: 12px;
}

.has-icon-prefix {
  padding-left: 40px;
}

.has-icon-suffix {
  padding-right: 40px;
}

/* Messages */
.tech-input-error {
  font-size: 0.75rem;
  color: var(--color-error);
}

.tech-input-help {
  font-size: 0.75rem;
  color: var(--tech-text-muted);
}
</style>
