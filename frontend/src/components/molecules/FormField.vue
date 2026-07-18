<script setup lang="ts">
defineProps<{ label: string }>();
</script>

<template>
  <label class="form-field">
    <span class="form-field__label">{{ label }}</span>
    <slot />
  </label>
</template>

<style scoped>
.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  margin-bottom: 0.75rem;
}

.form-field__label {
  font-size: 0.85rem;
  color: var(--muted);
  font-weight: 600;
}
</style>
