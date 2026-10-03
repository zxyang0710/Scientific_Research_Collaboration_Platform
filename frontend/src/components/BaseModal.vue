<script setup>
import { X } from 'lucide-vue-next'

defineProps({
  title: String,
  wide: Boolean
})

defineEmits(['close'])
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="$emit('close')">
    <section class="modal" :class="{ 'modal--wide': wide }" role="dialog" aria-modal="true">
      <header class="modal-header">
        <h2 class="modal-title">{{ title }}</h2>
        <button class="icon-btn" type="button" title="关闭" @click="$emit('close')">
          <X :size="19" />
        </button>
      </header>
      <div class="modal-body">
        <slot />
      </div>
      <footer v-if="$slots.footer" class="modal-footer">
        <slot name="footer" />
      </footer>
    </section>
  </div>
</template>
