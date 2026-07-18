<script setup lang="ts">
import type { IconName } from "../../models/iconName";
import { COLLECTION_VIEW_MODES, type CollectionViewMode } from "../../models/collectionView";
import Icon from "../atoms/Icon.vue";

const mode = defineModel<CollectionViewMode>({ required: true });

const MODE_ICONS: Record<CollectionViewMode, IconName> = {
  table: "table",
  list: "list",
  card: "cards",
};
</script>

<template>
  <div class="collection-view-toggle" role="group" :aria-label="$t('collectionView.label')">
    <button
      v-for="entry in COLLECTION_VIEW_MODES"
      :key="entry"
      type="button"
      class="collection-view-toggle__btn"
      :class="{ active: mode === entry }"
      :aria-label="$t(`collectionView.modes.${entry}`)"
      :title="$t(`collectionView.modes.${entry}`)"
      :aria-pressed="mode === entry"
      @click="mode = entry"
    >
      <Icon :name="MODE_ICONS[entry]" />
    </button>
  </div>
</template>

<style scoped>
.collection-view-toggle {
  display: inline-flex;
  border: 1px solid var(--border);
  border-radius: 0.4rem;
  overflow: hidden;
  background: var(--surface);
}

.collection-view-toggle__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.15rem;
  height: 2.15rem;
  border: none;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  padding: 0;
}

.collection-view-toggle__btn + .collection-view-toggle__btn {
  border-left: 1px solid var(--border);
}

.collection-view-toggle__btn:hover {
  color: var(--text);
  background: var(--accent-soft);
}

.collection-view-toggle__btn.active {
  color: var(--text);
  background: var(--accent-soft);
}
</style>
