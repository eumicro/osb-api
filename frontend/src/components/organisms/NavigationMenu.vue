<script setup lang="ts">
import type { PanelDefinition } from "../../workspace/panels";

defineProps<{
  items: readonly PanelDefinition[];
}>();

defineEmits<{
  activate: [panelId: string];
}>();
</script>

<template>
  <nav class="navigation-menu">
    <button
      v-for="item in items"
      :key="item.id"
      type="button"
      class="navigation-menu__item"
      @click="$emit('activate', item.id)"
    >
      {{ $t(item.titleKey) }}
    </button>
  </nav>
</template>

<style scoped>
.navigation-menu {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.navigation-menu__item {
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  color: var(--text);
  padding: 0.45rem 0.55rem;
  cursor: pointer;
  font: inherit;
}

.navigation-menu__item:hover {
  background: var(--accent-soft);
  border-color: var(--border);
}
</style>
