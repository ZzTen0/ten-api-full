<template>
  <span class="method-tag" :class="`method-tag--${methodLower}`">{{ method }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  method: {
    type: String,
    default: 'GET',
  },
})

const methodLower = computed(() => String(props.method || '').toLowerCase())
</script>

<style scoped>
.method-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  white-space: nowrap;
}

.method-tag--get {
  background: #ecfdf5;
  color: #059669;
}

.method-tag--post {
  background: #eff6ff;
  color: #2563eb;
}

.method-tag--put {
  background: #fffbeb;
  color: #d97706;
}

.method-tag--delete {
  background: #fef2f2;
  color: #dc2626;
}

.method-tag--patch {
  background: #f5f3ff;
  color: #7c3aed;
}
</style>
