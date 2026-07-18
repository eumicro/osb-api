<script setup lang="ts">
import Icon from "../atoms/Icon.vue";

/**
 * Molecule: ein-/ausklappbarer Inhaltsbereich (Header bleibt, Body optional).
 */
const open = defineModel<boolean>({ default: true });

defineProps<{
  title: string;
}>();
</script>

<template>
  <section class="collapsible-section" :class="{ 'is-open': open, 'is-collapsed': !open }">
    <header class="collapsible-section__header">
      <button
        type="button"
        class="collapsible-section__toggle"
        :aria-expanded="open"
        :aria-label="open ? $t('common.collapse') : $t('common.expand')"
        :title="open ? $t('common.collapse') : $t('common.expand')"
        @click="open = !open"
      >
        <Icon :name="open ? 'chevronDown' : 'chevronUp'" />
        <span class="collapsible-section__title">{{ title }}</span>
      </button>
    </header>
    <div v-show="open" class="collapsible-section__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.collapsible-section {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.collapsible-section.is-open {
  flex: 1 1 auto;
  overflow: hidden;
}

.collapsible-section.is-collapsed {
  flex: 0 0 auto;
}

.collapsible-section__header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  min-height: 2rem;
}

.collapsible-section__toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  width: 100%;
  padding: 0.35rem 0.15rem;
  border: none;
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}

.collapsible-section__toggle:hover {
  color: var(--text);
}

.collapsible-section__title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collapsible-section__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}
</style>
