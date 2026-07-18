<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { CollectionViewMode } from "../../models/collectionView";
import type { ServiceOffering } from "../../models/catalog";
import EntityCollection from "./EntityCollection.vue";

defineProps<{
  offerings: ServiceOffering[];
  selectedOfferingId: string | null;
  mode: CollectionViewMode;
  scope: string;
}>();

defineEmits<{
  select: [offering: ServiceOffering];
  remove: [offeringId: string];
  openInTab: [offering: ServiceOffering];
}>();

const { t } = useI18n();

const columns = computed(() => [
  { header: t("common.name"), value: (o: ServiceOffering) => o.name, primary: true },
  { header: t("common.description"), value: (o: ServiceOffering) => o.description },
  {
    header: t("offerings.plans"),
    value: (o: ServiceOffering) => String(o.plans.length),
  },
  {
    header: t("offerings.bindable"),
    value: (o: ServiceOffering) => (o.bindable ? t("common.yes") : t("common.no")),
  },
]);
</script>

<template>
  <EntityCollection
    :items="offerings"
    :columns="columns"
    :mode="mode"
    :scope="scope"
    :item-key="(o) => o.id"
    :selected-key="selectedOfferingId"
    :empty-label="t('offerings.none')"
    @select="$emit('select', $event)"
    @remove="$emit('remove', $event.id)"
    @open-in-tab="$emit('openInTab', $event)"
  />
</template>
