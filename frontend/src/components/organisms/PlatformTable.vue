<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { PlatformClient } from "../../models/platformClient";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  platforms: PlatformClient[];
  selectedPlatformClientId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [platform: PlatformClient];
  remove: [platformId: string];
  openInTab: [platform: PlatformClient];
}>();

const { t } = useI18n();

const columns = computed(() => [
  {
    header: t("common.name"),
    value: (p: PlatformClient) => p.displayName,
    primary: true,
  },
  { header: t("platforms.username"), value: (p: PlatformClient) => p.username },
  { header: t("platforms.catalog"), value: (p: PlatformClient) => p.catalogId },
  {
    header: t("common.status"),
    value: (p: PlatformClient) =>
      p.enabled ? t("platforms.enabled") : t("platforms.disabled"),
  },
]);
</script>

<template>
  <EntityCollection
    :items="platforms"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(p) => p.id"
    :selected-key="selectedPlatformClientId"
    :empty-label="t('platforms.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
