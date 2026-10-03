<script setup>
import BaseModal from './BaseModal.vue'

defineProps({
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定要执行此操作吗？' },
  confirmText: { type: String, default: '确认' },
  danger: Boolean,
  loading: Boolean
})

defineEmits(['confirm', 'close'])
</script>

<template>
  <BaseModal class="confirm-dialog" :title="title" @close="$emit('close')">
    <p style="color: var(--ink-700); line-height: 1.7">{{ message }}</p>
    <template #footer>
      <button class="btn btn-secondary" type="button" @click="$emit('close')">取消</button>
      <button
        class="btn"
        :class="danger ? 'btn-danger' : 'btn-primary'"
        type="button"
        :disabled="loading"
        @click="$emit('confirm')"
      >
        {{ loading ? '处理中...' : confirmText }}
      </button>
    </template>
  </BaseModal>
</template>
