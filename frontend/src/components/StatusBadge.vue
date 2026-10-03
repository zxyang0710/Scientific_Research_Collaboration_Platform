<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: String,
  map: { type: Object, default: () => ({}) }
})

const config = computed(() => props.map[props.value] || { label: props.value || '未知', tone: 'slate' })
</script>

<template>
  <span class="badge" :class="`badge--${config.tone}`">{{ config.label }}</span>
</template>
