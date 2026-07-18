<script setup lang="ts">
import type { SortDirection } from "../../models/collectionView";
import Icon from "../atoms/Icon.vue";

/**
 * Molecule: Tabellen-Spaltenkopf mit Sortieren (Klick) und Gruppieren (Icon).
 */
defineProps<{
  label: string;
  sortDirection: SortDirection | null;
  grouped: boolean;
  sortable?: boolean;
  groupable?: boolean;
}>();

const emit = defineEmits<{
  sort: [];
  group: [];
}>();
</script>

<template>
  <div class="table-column-header">
    <button
      v-if="sortable !== false"
      type="button"
      class="table-column-header__sort"
      :class="{ active: sortDirection != null }"
      :aria-label="label"
      :title="label"
      @click="emit('sort')"
    >
      <span class="table-column-header__label">{{ label }}</span>
      <span class="table-column-header__sort-icon" aria-hidden="true">
        <Icon v-if="sortDirection === 'asc'" name="chevronUp" />
        <Icon v-else-if="sortDirection === 'desc'" name="chevronDown" />
        <span v-else class="table-column-header__sort-placeholder" />
      </span>
    </button>
    <span v-else class="table-column-header__label">{{ label }}</span>
    <button
      v-if="groupable !== false"
      type="button"
      class="table-column-header__group"
      :class="{ active: grouped }"
      :aria-label="grouped ? $t('collectionTable.ungroup') : $t('collectionTable.groupBy')"
      :title="grouped ? $t('collectionTable.ungroup') : $t('collectionTable.groupBy')"
      :aria-pressed="grouped"
      @click.stop="emit('group')"
    >
      <Icon name="group" />
    </button>
  </div>
</template>

<style scoped>
.table-column-header {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  min-width: 0;
}

.table-column-header__sort {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}

.table-column-header__sort:hover,
.table-column-header__sort.active {
  color: var(--text);
}

.table-column-header__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-column-header__sort-icon {
  display: inline-flex;
  width: 0.95em;
  height: 0.95em;
  flex: 0 0 auto;
  opacity: 0.55;
}

.table-column-header__sort.active .table-column-header__sort-icon {
  opacity: 1;
  color: var(--accent);
}

.table-column-header__sort-placeholder {
  display: block;
  width: 0.95em;
  height: 0.95em;
}

.table-column-header__group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.35rem;
  height: 1.35rem;
  padding: 0;
  border: none;
  border-radius: 0.3rem;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  flex: 0 0 auto;
  opacity: 0;
}

.table-column-header:hover .table-column-header__group,
.table-column-header__group.active,
.table-column-header__group:focus-visible {
  opacity: 1;
}

.table-column-header__group:hover {
  color: var(--text);
  background: var(--accent-soft);
}

.table-column-header__group.active {
  color: var(--accent);
  background: var(--accent-soft);
}
</style>
