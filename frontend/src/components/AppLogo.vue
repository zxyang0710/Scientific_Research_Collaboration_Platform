<script setup>
import { FlaskConical } from 'lucide-vue-next'

defineProps({
  compact: Boolean,
  light: Boolean
})
</script>

<template>
  <div class="app-logo" :class="{ 'app-logo--light': light }">
    <span class="app-logo__mark">
      <FlaskConical :size="21" :stroke-width="2.2" />
    </span>
    <span v-if="!compact" class="app-logo__copy">
      <strong>研协</strong>
      <small>校园科研协作平台</small>
    </span>
  </div>
</template>

<style scoped>
.app-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.app-logo__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: var(--teal-700);
  color: white;
}

.app-logo__copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.app-logo__copy strong {
  color: var(--ink-950);
  font-size: 19px;
  line-height: 1.1;
  letter-spacing: 0.08em;
}

.app-logo__copy small {
  margin-top: 2px;
  color: var(--ink-500);
  font-size: 10px;
  white-space: nowrap;
}

.app-logo--light .app-logo__mark {
  background: rgba(255, 255, 255, 0.14);
  color: white;
}

.app-logo--light .app-logo__copy strong {
  color: white;
}

.app-logo--light .app-logo__copy small {
  color: rgba(255, 255, 255, 0.62);
}
</style>
