<script setup lang="ts">
/**
 * Molecule: standard list-panel toolbar (primary actions left, tools right).
 */
</script>

<template>
  <div class="panel-toolbar">
    <div class="panel-toolbar__start">
      <slot />
    </div>
    <div class="panel-toolbar__end">
      <slot name="end" />
    </div>
  </div>
</template>

<style scoped>
.panel-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.panel-toolbar__start,
.panel-toolbar__end {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>
