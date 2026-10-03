<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  src: { type: String, default: '' },
  size: { type: String, default: 'md' }
})

const initial = computed(() => (props.name || '?').trim().slice(0, 1).toUpperCase())
</script>

<template>
  <span class="avatar" :class="`avatar--${size}`">
    <img v-if="src" :src="src" :alt="name" />
    <span v-else>{{ initial }}</span>
  </span>
</template>
