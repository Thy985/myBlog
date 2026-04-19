<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :show-close="showClose"
    draggable
    destroy-on-close
    class="form-dialog"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-position="labelPosition"
      :size="size"
      v-bind="$attrs"
    >
      <slot>
        <!-- 表单项插槽 -->
      </slot>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          {{ confirmText }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '表单'
  },
  width: {
    type: [String, Number],
    default: '40%'
  },
  showClose: {
    type: Boolean,
    default: true
  },
  formData: {
    type: Object,
    default: () => ({})
  },
  rules: {
    type: Object,
    default: () => ({})
  },
  labelPosition: {
    type: String,
    default: 'top'
  },
  size: {
    type: String,
    default: 'default'
  },
  confirmText: {
    type: String,
    default: '提交'
  },
  submitLoading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'update:modelValue',
  'submit'
])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref(null)

function handleSubmit() {
  emit('submit', formRef.value)
}
</script>

<style scoped>
.form-dialog :deep(.el-dialog__body) {
  padding-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
