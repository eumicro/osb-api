<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { AdminCatalog } from "../../models/catalog";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  catalogs: AdminCatalog[];
  selectedCatalogId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [catalog: AdminCatalog];
  remove: [catalogId: string];
  openInTab: [catalog: AdminCatalog];
}>();

const { t } = useI18n();

const columns = computed(() => [
  { header: t("common.id"), value: (c: AdminCatalog) => c.id, primary: true },
  { header: t("common.name"), value: (c: AdminCatalog) => c.name },
  { header: t("common.description"), value: (c: AdminCatalog) => c.description },
  {
    header: t("catalogs.offerings"),
    value: (c: AdminCatalog) => String(c.offerings.length),
  },
]);
</script>

<template>
  <EntityCollection
    :items="catalogs"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(c) => c.id"
    :selected-key="selectedCatalogId"
    :empty-label="t('catalogs.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
