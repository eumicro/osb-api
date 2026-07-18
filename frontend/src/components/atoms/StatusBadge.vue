<script setup lang="ts">
defineProps<{ label: string }>();
</script>

<template>
  <span class="status-badge">{{ label }}</span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--ok, #3ecf8e);
  font-size: 0.9rem;
}

.status-badge::before {
  content: "";
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 50%;
  background: currentColor;
}
</style>
