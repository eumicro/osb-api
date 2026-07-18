<script setup lang="ts">
import Icon from "../atoms/Icon.vue";

/** Molecule: icon link that opens a URL in a new browser tab. */
defineProps<{
  href: string;
  label: string;
}>();
</script>

<template>
  <a
    class="open-in-new-tab"
    :href="href"
    target="_blank"
    rel="noopener noreferrer"
    :aria-label="label"
    :title="label"
  >
    <Icon name="tab" />
  </a>
</template>

<style scoped>
.open-in-new-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  width: 2.15rem;
  height: 2.15rem;
  padding: 0;
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--border);
  text-decoration: none;
}

.open-in-new-tab:hover {
  background: var(--accent-soft);
}
</style>
