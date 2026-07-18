<script setup lang="ts">
defineProps<{
  entries: Record<string, string>;
}>();
</script>

<template>
  <div class="key-value-list">
    <div v-for="(value, key) in entries" :key="key" class="row">
      <span>{{ key }}</span>
      <span class="muted mono">{{ value }}</span>
    </div>
  </div>
</template>

<style scoped>
.key-value-list {
  display: grid;
  gap: 0.45rem;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px dashed var(--border);
  padding: 0.35rem 0;
  font-size: 0.92rem;
}

.mono {
  font-family: ui-monospace, monospace;
}
</style>
